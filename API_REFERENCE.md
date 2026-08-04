# Receipt Handler API — Frontend Reference

Backend for uploading receipt images/PDFs, extracting their contents with an AI vision
model, and reporting on spending. This document describes every HTTP endpoint, the
request/response shapes, auth model, and conventions the frontend needs.

> Generated from the Spring controllers/DTOs. For the machine-readable contract, the live
> OpenAPI spec is at `GET /v3/api-docs` and Swagger UI at `/swagger-ui.html`.

---

## 1. Base URL, host & CORS

- **Base URL (local dev):** `http://localhost:8080` (default Spring port; no `server.port` override).
- **All routes are prefixed** under `/api/...`.
- **CORS:** the backend only allows origin **`http://localhost:5173`** (Vite's default dev port),
  with credentials enabled. If your frontend runs on a different port, the backend's
  `SecurityConfig` CORS list must be updated or requests will be blocked by the browser.
- **Credentials matter:** the refresh token is delivered as an **HttpOnly cookie**, so every
  auth-related request that should send/receive that cookie must use credentialed requests:
  - `fetch(url, { credentials: 'include' })`
  - or axios `withCredentials: true`

---

## 2. Authentication model

Two tokens are in play:

| Token | Where it lives | Lifetime | Sent how |
|-------|----------------|----------|----------|
| **Access token (JWT)** | Returned in the JSON body (`token`) — store in memory/state | ~2.5 h | `Authorization: Bearer <token>` header on every protected request |
| **Refresh token** | **HttpOnly cookie** named `refreshToken`, set by the server | 7 days | Sent automatically by the browser to `/api/auth/refresh` (needs credentialed requests) |

**Flow:**
1. `POST /api/auth/register` or `/api/auth/login` → returns access token in body + sets refresh cookie.
2. Store the access token in memory (not localStorage, ideally). Attach it as `Authorization: Bearer <token>` to all protected calls.
3. When the access token expires (a protected call returns **401**), call `POST /api/auth/refresh`
   (no body needed — the cookie travels automatically). You get a **new access token** and the
   refresh cookie is **rotated**.
4. `POST /api/auth/logout` revokes the current access token and clears the refresh cookie.

**Public endpoints (no token required):** everything under `/api/auth/**`, plus `/actuator/health`,
`/v3/api-docs/**`, `/swagger-ui/**`. **Every other endpoint returns 401 without a valid Bearer token.**

---

## 3. Conventions

- **Request/response bodies** are JSON (`Content-Type: application/json`), except file uploads which
  are `multipart/form-data`, and the Excel export which returns a binary file.
- **IDs** are UUID strings, e.g. `"a3f1c2d4-5e6f-7890-ab12-cd34ef56ab78"`.
- **Money** fields are JSON numbers with 2 decimals, e.g. `12500.00`. Currency is always `"NGN"`.
- **Dates:** `receiptDate` / `date` are calendar dates `"2026-06-15"`. `createdAt` is an ISO-8601
  instant `"2026-06-15T10:30:00Z"`. Error `timestamp` is a local date-time `"2026-06-15T10:30:00"`.
- **Nullable extraction fields:** a freshly uploaded receipt has `merchantName`, `totalAmount`,
  `receiptDate`, and `items` empty/null until AI processing finishes (see async model below).

### Error shape (all 4xx/5xx)

```json
{
  "error": "INVALID_FILE_FORMAT",
  "message": "Please upload a JPEG, PNG, WEBP, or PDF file.",
  "timestamp": "2026-06-15T10:30:00"
}
```

### Paging wrapper

List endpoints return a **paged envelope**, not a bare array:

```json
{
  "content": [ /* array of items for this page */ ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "last": false
}
```

Query params for paged endpoints: `page` (zero-based, default `0`) and `size`
(default `20`, **capped at 100**). Ordering is fixed newest-first; there is no client `sort` param.

---

## 4. Async receipt processing model

Uploads are processed in the background. **An upload does not return the extracted data.**

1. `POST /api/receipts/upload` (or `/upload-batch`) returns **202 Accepted** immediately, with the
   receipt in status `PENDING` or `PROCESSING` and empty extraction fields.
2. **Poll** `GET /api/receipts/{id}` until `status` becomes `COMPLETED` (data populated) or
   `FAILED` (see `errorMessage`).
3. A background reaper marks receipts stuck in `PROCESSING` too long as `FAILED`.

`ProcessingStatus` values: `PENDING` → `PROCESSING` → `COMPLETED` | `FAILED`.

---

## 5. Endpoints

### 5.1 Authentication — `/api/auth` (public)

#### `POST /api/auth/register`
Create an account and sign in. **201 Created.**

Request body (`RegisterRequest`):
```json
{ "email": "yesirat@example.com", "password": "s3curePass!", "fullName": "Yesirat Bello" }
```
Validation: `email` valid & unique; `password` ≥ 8 chars; `fullName` 2–100 chars.

Response body (`AuthResponse`) + `Set-Cookie: refreshToken=...; HttpOnly`:
```json
{ "token": "eyJhbGciOi...", "email": "yesirat@example.com", "fullName": "Yesirat Bello" }
```
Errors: `400` validation, `409` email already registered.

#### `POST /api/auth/login`
Authenticate. **200 OK.** Body (`LoginRequest`):
```json
{ "email": "yesirat@example.com", "password": "s3curePass!" }
```
Returns `AuthResponse` + refresh cookie. Error: `401` invalid credentials.

#### `POST /api/auth/refresh`
Exchange the refresh cookie for a new access token. **No request body.** Requires credentialed
request so the `refreshToken` cookie is sent. **200 OK** → new `AuthResponse` + rotated cookie.
Error: `401` missing/expired/invalid refresh token.

#### `POST /api/auth/logout`
Revoke the current access token and clear the refresh cookie. Safe to call with or without a token.
**204 No Content.** (Optionally send the `Authorization` header so the access token is revoked.)

---

### 5.2 Receipts — `/api/receipts` (Bearer required)

#### `POST /api/receipts/upload`  — `multipart/form-data`
Upload a single receipt. **202 Accepted** → `ReceiptResponseDTO` in `PENDING`/`PROCESSING`.

Form field: `file` — JPEG, PNG, WEBP, or PDF; **max 5 MB**.
Errors: `400` missing/unsupported file, `401` no token.

#### `POST /api/receipts/upload-batch`  — `multipart/form-data`
Upload many receipts for one staff member. **Always 202** (even if some files fail).

Form fields:
- `staffId` — UUID of the staff member.
- `files` — one or more files (each JPEG/PNG/WEBP/PDF, max 5 MB).

Response (`BatchUploadResponseDTO`):
```json
{
  "accepted": [ /* ReceiptResponseDTO[] queued for processing */ ],
  "rejected": [ { "fileName": "blurry-scan.png", "reason": "Please upload a JPEG, PNG, WEBP, or PDF file." } ]
}
```

#### `GET /api/receipts`  — **paged**
The authenticated user's receipts, newest first.
Query: `page` (default 0), `size` (default 20, max 100).
Returns `PagedResponse<ReceiptResponseDTO>`.

#### `GET /api/receipts/{id}`
One receipt by ID (owned by the user), including items and status. Use this to poll after upload.
`200` → `ReceiptResponseDTO`; `404` if not found for this user.

#### `GET /api/receipts/summary`
Monthly spending summary broken down by category.
Query: `month` — `yyyy-MM` (e.g. `2026-06`); defaults to the current month if omitted.
`200` → `SpendingSummary`.

#### `GET /api/receipts/items`  — **paged**
Individual line items across all receipts in a category (answers "what did I spend on X"), newest first.
Query: `category` (**required**, a `Category` enum value), `page` (default 0), `size` (default 20, max 100).
Returns `PagedResponse<ReceiptItemWithContextDTO>`.

#### `GET /api/receipts/export`
Download the month's completed receipts as an `.xlsx` workbook.
Query: `month` — `yyyy-MM`; defaults to current month.
`200` → binary body with
`Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` and a
`Content-Disposition: attachment; filename="..."` header. Handle as a file download (blob), not JSON.

---

### 5.3 Staff — `/api/staff` (Bearer required)

#### `GET /api/staff`
Search staff (typeahead). Query: `query` — optional case-insensitive name fragment; omit to list all.
`200` → `StaffResponseDTO[]` (bare array, **not** paged).

#### `POST /api/staff`
Find-or-create a staff member by name. Idempotent by name — call this when a typed name doesn't
match an existing member. `200` → `StaffResponseDTO`.
Request body:
```json
{ "name": "Yesirat Bello" }
```

---

## 6. Data models (JSON shapes)

### ReceiptResponseDTO
```jsonc
{
  "id": "uuid",
  "merchantName": "Shoprite",        // null until COMPLETED
  "totalAmount": 12500.00,           // null until COMPLETED
  "currency": "NGN",
  "receiptDate": "2026-06-15",       // null if undated / not yet extracted
  "createdAt": "2026-06-15T10:30:00Z",
  "items": [ /* ReceiptItemDTO[]; empty until COMPLETED */ ],
  "status": "PENDING",               // PENDING | PROCESSING | COMPLETED | FAILED
  "errorMessage": null               // set only when status = FAILED
}
```

### ReceiptItemDTO
```jsonc
{ "id": "uuid", "name": "Milk 1L", "amount": 1500.00, "category": "OTHER" }
```

### ReceiptItemWithContextDTO  (returned by `/api/receipts/items`)
```jsonc
{
  "itemId": "uuid",
  "name": "Eggs (dozen)",
  "amount": 2800.00,
  "category": "OTHER",
  "receiptId": "uuid",
  "merchantName": "Shoprite",
  "receiptDate": "2026-06-15"
}
```

### SpendingSummary  (returned by `/api/receipts/summary`)
```jsonc
{
  "totalSpend": 84500.00,
  "currency": "NGN",
  "period": "JUNE 2026",
  "breakdown": [
    { "category": "DIESEL", "amount": 32000.00, "count": 14 }
  ],
  "unknownDateTotal": 5000.00,   // receipts whose date couldn't be determined
  "unknownDateCount": 2
}
```

### StaffResponseDTO
```jsonc
{ "id": "uuid", "name": "Yesirat Bello", "active": true }
```

### AuthResponse
```jsonc
{ "token": "jwt-access-token", "email": "yesirat@example.com", "fullName": "Yesirat Bello" }
```

### PagedResponse<T>
```jsonc
{ "content": [ /* T[] */ ], "page": 0, "size": 20, "totalElements": 137, "totalPages": 7, "last": false }
```

### ErrorResponse
```jsonc
{ "error": "CODE", "message": "Human-readable message.", "timestamp": "2026-06-15T10:30:00" }
```

---

## 7. Enums

### Category
Used on items and in category filters/summaries. Values:

```
VEHICLE_MAINTENANCE
TRANSPORTATION
INTERNET_DATA_BUNDLE
LAUNDRY_OF_OFFICE_WEAR
HEALTH_AND_WELLNESS
DIESEL
ENTERTAINMENT_MARKETING
STAFF_TRAINING
OTHER
```
Unknown/unclassifiable values map to `OTHER`.

### ProcessingStatus
```
PENDING · PROCESSING · COMPLETED · FAILED
```

---

## 8. Quick frontend integration notes

- **Axios instance:** set `baseURL: 'http://localhost:8080'`, `withCredentials: true`, and an
  interceptor that adds `Authorization: Bearer <accessToken>` from your in-memory store.
- **401 handling:** on any protected `401`, call `/api/auth/refresh` once, retry the original
  request with the new token; if refresh also 401s, redirect to login.
- **Upload:** build `FormData`, append `file` (single) or `staffId` + multiple `files` (batch); do
  **not** set `Content-Type` manually — let the browser set the multipart boundary.
- **Post-upload UX:** show the receipt as "Processing", then poll `GET /api/receipts/{id}` (e.g.
  every 2–3 s, with a timeout) until `status` is `COMPLETED` or `FAILED`.
- **Excel export:** request as a blob and trigger a download using the `Content-Disposition` filename.
- **Staff typeahead:** call `GET /api/staff?query=...`; if the user's typed name has no match, `POST
  /api/staff` to create-or-get, then use the returned `id` as `staffId` for batch upload.
```

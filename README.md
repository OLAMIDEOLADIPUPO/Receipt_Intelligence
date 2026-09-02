# ReceiptHandler

ReceiptHandler is the backend service for ClearSpend, an internal expense reimbursement platform. It ingests receipt images and PDFs, extracts structured purchase data from them using a configurable AI vision provider, and exposes that data through a REST API for review, categorization, and monthly export.

The service is built with Spring Boot and Java 21, backed by PostgreSQL, and secured with stateless JWT authentication.

## Table of Contents

- [Overview](#overview)
- [Core Capabilities](#core-capabilities)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Background Jobs](#background-jobs)
- [Security Notes](#security-notes)
- [Project Structure](#project-structure)
- [Testing](#testing)

## Overview

Two groups of people interact with the system:

Accounts staff authenticate with a username and password and use the API to manage a staff roster, review submitted receipts, filter them by category or month, and export a completed month as an Excel workbook.

Employees submit their own receipts through a public, unauthenticated endpoint by identifying themselves with their name and an employee ID that Accounts has already registered. No employee account or login exists in this system; identity is validated against the roster at submission time.

Every submitted receipt is queued for asynchronous processing. A configurable AI vision provider reads the image or PDF, extracts merchant name, transaction date, total amount, and itemized line items, and assigns each line item to one of a fixed set of expense categories. The receipt then transitions to a completed or failed state depending on whether extraction succeeded.

## Core Capabilities

- JWT based authentication with short lived access tokens, rotating refresh tokens delivered as an HttpOnly cookie, and server side revocation on logout.
- Asynchronous receipt processing backed by a dedicated thread pool, with a scheduled reaper that fails receipts stuck in processing beyond a configurable timeout.
- A provider agnostic AI extraction layer. Google Gemini, Anthropic Claude, and Groq are implemented behind a single interface and are switchable through configuration without a code change.
- Automatic image downscaling before receipts are sent to the AI provider, reducing token usage without materially affecting extraction accuracy.
- A public staff self-upload endpoint gated by a configurable submission window, defaulting to the 10th through the 15th of each month, with a manual override Accounts can use to open, close, or pause submissions at any time.
- Category based and month based filtering of the receipts list, and a company wide monthly spending summary.
- Excel export of a given month's completed receipts, with defenses against formula injection in exported cell values.
- Full OpenAPI 3 documentation, served interactively through Swagger UI.

## Architecture

The codebase follows a conventional layered structure.

Controllers accept HTTP requests, perform request level validation, and delegate to services. Services contain business logic and orchestrate repositories, the async processor, and the AI extraction layer. Repositories are Spring Data JPA interfaces backed by PostgreSQL. Models are JPA entities. DTOs define the request and response contracts exposed by the API, kept separate from persistence entities.

Receipt processing is asynchronous by design. An upload request persists a placeholder receipt row and returns immediately with a pending status. A background task then calls the configured AI provider, parses its response, and updates the receipt with extracted data or a failure reason. Clients are expected to poll the receipt by ID until it reaches a terminal state.

The AI extraction layer is defined by a single `ReceiptExtractionService` interface. Each provider, Gemini, Claude, and Groq, implements this interface independently but shares a common request retry policy, a common defensive JSON parser for cleaning up provider responses, and a single prompt definition that all three providers are given verbatim. The active provider is selected at startup through configuration, with a fail fast check if the selected provider is missing required credentials.

## Technology Stack

| Concern | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 |
| Persistence | Spring Data JPA, PostgreSQL |
| Authentication | Spring Security, JJWT |
| API documentation | springdoc-openapi (OpenAPI 3, Swagger UI) |
| Excel export | Apache POI |
| Build tool | Maven |
| AI providers | Google Gemini, Anthropic Claude, Groq |

## Prerequisites

- Java Development Kit 21 or later
- PostgreSQL 14 or later, running locally or reachable over the network
- An API key for at least one supported AI provider (Gemini, Claude, or Groq)

The project includes the Maven Wrapper, so a separate Maven installation is not required.

## Getting Started

Clone the repository and move into the project directory.

```bash
git clone https://github.com/OLAMIDEOLADIPUPO/Receipt_Intelligence.git
cd Receipt_Intelligence
```

Create a PostgreSQL database for the application. The default configuration expects a database named `receipt_remita` on `localhost:5432`.

```sql
CREATE DATABASE receipt_remita;
```

Create a `.env` file at the project root with the required configuration values described in [Configuration](#configuration). Spring Boot loads this file automatically through `spring.config.import`.

Seed the placeholder system user that every self-uploaded receipt is attached to, since the self-upload flow has no authenticated user of its own. This is a one time setup step per environment.

```sql
INSERT INTO users (email, password, full_name, created_at)
VALUES (
  'staff-self-upload@system.local',
  '$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZabcd',
  'Staff Self-Upload',
  now()
);
```

The password value is never used to authenticate, since this row is never logged into directly, so any valid bcrypt formatted string is acceptable. Run this after the application has started at least once with `ddl-auto: update`, so the `users` table has already been created.

Run the application.

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`. The interactive API documentation is available at `http://localhost:8080/swagger-ui.html`.

## Configuration

All configuration is provided through environment variables, typically via a `.env` file at the project root. Values without a default are required.

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_PASSWORD` | Yes | none | Password for the PostgreSQL user configured in `application.yaml`. |
| `JWT_SECRET_KEY` | Yes | none | Base64 encoded HMAC signing key for access and refresh tokens. Must be at least 256 bits. |
| `RECEIPT_EXTRACTION_PROVIDER` | No | `groq` | Selects the active AI provider. One of `gemini`, `claude`, or `groq`. |
| `GEMINI_API_KEY` | Only if provider is `gemini` | none | API key for Google Gemini. |
| `GEMINI_API_URL` | Only if provider is `gemini` | none | Full endpoint URL for the Gemini `generateContent` call. |
| `CLAUDE_API_KEY` | Only if provider is `claude` | none | API key for Anthropic Claude. |
| `CLAUDE_API_URL` | No | `https://api.anthropic.com/v1/messages` | Claude Messages API endpoint. |
| `CLAUDE_API_MODEL` | No | `claude-sonnet-5` | Claude model identifier. |
| `CLAUDE_API_MAX_TOKENS` | No | `1024` | Maximum response tokens for Claude. |
| `GROQ_API_KEY` | Only if provider is `groq` | none | API key for Groq. |
| `GROQ_API_URL` | No | `https://api.groq.com/openai/v1/chat/completions` | Groq chat completions endpoint. |
| `GROQ_API_MODEL` | No | `qwen/qwen3.6-27b` | Groq model identifier. |
| `GROQ_API_MAX_TOKENS` | No | `1024` | Maximum response tokens for Groq. |

The application fails fast at startup if `RECEIPT_EXTRACTION_PROVIDER` selects a provider whose required API key is not set.

Additional application behavior, including the JWT token lifetimes, the scheduled job intervals, and the CORS allowed origin, is defined in `src/main/resources/application.yaml`.

## API Documentation

The full HTTP API, including request and response shapes, authentication requirements, and error codes, is documented in [API_REFERENCE.md](API_REFERENCE.md).

A machine readable OpenAPI 3 specification is served at `/v3/api-docs`, and an interactive Swagger UI is served at `/swagger-ui.html`, once the application is running.

## Background Jobs

The application runs two scheduled jobs, both configurable under `app.jobs` in `application.yaml`.

The revoked token cleanup job periodically removes expired entries from the revoked access token table, preventing unbounded growth of that table over time.

The stuck receipt reaper periodically scans for receipts that have remained in a processing state longer than a configured timeout, typically because the AI provider call failed in a way that was not cleanly handled, and marks them as failed so they do not remain invisible to Accounts.

## Security Notes

Access tokens are short lived JSON Web Tokens sent as a bearer token on every authenticated request. Refresh tokens are longer lived, delivered exclusively as an HttpOnly cookie, and rotated on every use. Reuse of an already rotated refresh token is treated as a signal of token theft and revokes the entire token family for that user.

The login endpoint enforces a per IP address rate limit to reduce the effectiveness of password guessing attacks.

Values written into exported Excel workbooks are sanitized against formula injection, since some of that data originates from AI extracted text on user submitted images.

CORS is restricted to a single configured origin, matching the frontend's development URL. This must be updated before deploying to a different origin.

## Project Structure

```
src/main/java/com/olamide/receipthandler/
  configurations/    Spring configuration: security, JWT, CORS, async, OpenAPI
  components/        Scheduled jobs and small stateless helpers
  controllers/        REST controllers
  dto/               Request and response payloads
  enums/             Fixed value sets such as ProcessingStatus and Category
  exceptions/        Domain exceptions and the global exception handler
  models/            JPA entities
  repository/        Spring Data JPA repositories
  service/           Service interfaces and the AI provider abstraction
  service/serviceImpl/  Service implementations, including each AI provider client
  service/async/     Asynchronous receipt processing
  utilities/         Stateless helpers such as image resizing
```

## Testing

Run the test suite with the Maven Wrapper.

```bash
./mvnw test
```

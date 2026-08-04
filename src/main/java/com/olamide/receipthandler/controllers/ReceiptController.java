package com.olamide.receipthandler.controllers;

import com.olamide.receipthandler.dto.*;
import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.exceptions.ErrorResponse;
import com.olamide.receipthandler.service.ReceiptExcelExportService;
import com.olamide.receipthandler.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/receipts")
@Tag(name = "Receipts", description = "Upload receipts for AI extraction, browse them, and report on spending. Requires a Bearer access token.")
public class ReceiptController {
    private final ReceiptService receiptService;
    private final ReceiptExcelExportService receiptExcelExportService;

    public ReceiptController(ReceiptService receiptService, ReceiptExcelExportService receiptExcelExportService) {
        this.receiptService = receiptService;
        this.receiptExcelExportService = receiptExcelExportService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a single receipt",
            description = "Accepts a JPEG, PNG, WEBP, or PDF (max 5 MB) and queues it for asynchronous AI "
                    + "extraction. Returns 202 immediately with the receipt in `PENDING`/`PROCESSING` state; "
                    + "poll `GET /api/receipts/{id}` until the status becomes `COMPLETED` or `FAILED`.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Missing file or unsupported file type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
    })
    public ResponseEntity<ReceiptResponseDTO> uploadReceipt(
            @Parameter(description = "Receipt image or PDF (JPEG, PNG, WEBP, or PDF; max 5 MB)")
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(receiptService.processReceipt(file));
    }

    @PostMapping(value = "/upload-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a batch of receipts for one staff member",
            description = "Uploads multiple receipt files at once, tagging every one to the given staff member. "
                    + "Each file is validated and queued independently: the response lists the accepted receipts "
                    + "and, separately, any files rejected (with the reason). Always 202 even if some files fail.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Batch accepted; see body for per-file outcomes"),
            @ApiResponse(responseCode = "400", description = "No files supplied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
    })
    public ResponseEntity<BatchUploadResponseDTO> uploadBatch(
            @Parameter(description = "ID of the staff member the receipts belong to")
            @RequestParam("staffId") UUID staffId,
            @Parameter(description = "One or more receipt files (JPEG, PNG, WEBP, or PDF; max 5 MB each)")
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(receiptService.processBatch(staffId, files));
    }

    @GetMapping
    @Operation(summary = "List receipts (paged)",
            description = "Returns the authenticated user's receipts, newest first, one page at a time. "
                    + "Walk the pages with `page` and `size`; the response carries `totalElements`, "
                    + "`totalPages`, and `last` so you know when to stop.")
    @ApiResponse(responseCode = "200", description = "Page of receipts returned")
    public ResponseEntity<PagedResponse<ReceiptResponseDTO>> getAllReceipts(
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Receipts per page (1–100; defaults to 20, capped at 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(receiptService.getAllReceipts(page, size));
    }

    @GetMapping("/summary")
    @Operation(summary = "Monthly spending summary",
            description = "Aggregates spending for a month, broken down by category, plus a separate total for "
                    + "receipts whose date couldn't be determined.")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    public ResponseEntity<SpendingSummary> getSpendingSummary(
            @Parameter(description = "Target month as `yyyy-MM` (e.g. 2026-06). Defaults to the current month if omitted.",
                    example = "2026-06")
            @RequestParam(required = false) String month) {

        YearMonth target = (month != null && !month.isBlank())
                ? YearMonth.parse(month)
                : YearMonth.now();

        return ResponseEntity.ok(receiptService.getSpendingSummary(target));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a receipt by ID",
            description = "Fetches a single receipt owned by the authenticated user, including its extracted items "
                    + "and current processing status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt found"),
            @ApiResponse(responseCode = "404", description = "No receipt with this ID for the current user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReceiptResponseDTO> getReceiptById(
            @Parameter(description = "Receipt ID") @PathVariable UUID id) {
        return ResponseEntity.ok(receiptService.getReceiptById(id));
    }

    // Answers "what did I spend on category X", independent of which
    // receipts it came from — distinct from GET /api/receipts, which
    // always returns whole receipts.
    @GetMapping("/items")
    @Operation(summary = "List items in a category (paged)",
            description = "Returns individual line items across all receipts for the given category, newest first, "
                    + "each carrying its parent receipt's merchant and date. Answers \"what did I spend on X\" "
                    + "regardless of which receipt each item came from. Walk the pages with `page` and `size`.")
    @ApiResponse(responseCode = "200", description = "Page of items returned")
    public ResponseEntity<PagedResponse<ReceiptItemWithContextDTO>> getItemsByCategory(
            @Parameter(description = "Spending category to filter items by")
            @RequestParam Category category,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page (1–100; defaults to 20, capped at 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(receiptService.getItemsByCategory(category, page, size));
    }

    @GetMapping("/export")
    @Operation(summary = "Export a month's receipts to Excel",
            description = "Generates an `.xlsx` workbook of the month's completed receipts and returns it as a file "
                    + "download (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`).")
    @ApiResponse(responseCode = "200", description = "Excel workbook returned as an attachment",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    public ResponseEntity<byte[]> exportMonth(
            @Parameter(description = "Target month as `yyyy-MM` (e.g. 2026-06). Defaults to the current month if omitted.",
                    example = "2026-06")
            @RequestParam(required = false) String month) {
        YearMonth target = (month != null && !month.isBlank())
                ? YearMonth.parse(month)
                : YearMonth.now();

        ExcelExportResult result = receiptExcelExportService.exportMonth(target);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(result.content());
    }
}
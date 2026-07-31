package com.olamide.receipthandler.controllers;

import com.olamide.receipthandler.dto.*;
import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.service.ReceiptExcelExportService;
import com.olamide.receipthandler.service.ReceiptService;
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
public class ReceiptController {
    private final ReceiptService receiptService;
    private final ReceiptExcelExportService receiptExcelExportService;

    public ReceiptController(ReceiptService receiptService, ReceiptExcelExportService receiptExcelExportService) {
        this.receiptService = receiptService;
        this.receiptExcelExportService = receiptExcelExportService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ReceiptResponseDTO> uploadReceipt(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(receiptService.processReceipt(file));
    }

    @PostMapping("/upload-batch")
    public ResponseEntity<BatchUploadResponseDTO> uploadBatch(
            @RequestParam("staffId") UUID staffId,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(receiptService.processBatch(staffId, files));
    }

    @GetMapping
    public ResponseEntity<List<ReceiptResponseDTO>> getAllReceipts() {
        return ResponseEntity.ok(receiptService.getAllReceipts());
    }

    @GetMapping("/summary")
    public ResponseEntity<SpendingSummary> getSpendingSummary(
            @RequestParam(required = false) String month) {

        YearMonth target = (month != null && !month.isBlank())
                ? YearMonth.parse(month)
                : YearMonth.now();

        return ResponseEntity.ok(receiptService.getSpendingSummary(target));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponseDTO> getReceiptById(@PathVariable UUID id) {
        return ResponseEntity.ok(receiptService.getReceiptById(id));
    }

    // Answers "what did I spend on category X", independent of which
    // receipts it came from — distinct from GET /api/receipts, which
    // always returns whole receipts.
    @GetMapping("/items")
    public ResponseEntity<List<ReceiptItemWithContextDTO>> getItemsByCategory(
            @RequestParam Category category) {
        return ResponseEntity.ok(receiptService.getItemsByCategory(category));
    }
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMonth(@RequestParam(required = false) String month) {
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
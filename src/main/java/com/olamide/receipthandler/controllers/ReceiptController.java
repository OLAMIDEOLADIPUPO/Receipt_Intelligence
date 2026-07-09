package com.olamide.receipthandler.controllers;

import com.olamide.receipthandler.dto.ReceiptResponseDTO;
import com.olamide.receipthandler.dto.SpendingSummary;
import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.service.ReceiptService;
import org.springframework.http.HttpStatus;
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

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ReceiptResponseDTO> uploadReceipt(@RequestParam("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(receiptService.processReceipt(file));
    }

    @GetMapping
    public ResponseEntity<List<ReceiptResponseDTO>> getAllReceipts(@RequestParam(value = "category",required = false) Category category) {
        return ResponseEntity.ok(receiptService.getAllReceipts(category));
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
    }


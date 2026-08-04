package com.olamide.receipthandler.dto;

import com.olamide.receipthandler.enums.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "A receipt and its extracted contents. Extraction fields are null until processing completes.")
public record ReceiptResponseDTO(
        @Schema(description = "Receipt ID") UUID id,
        @Schema(description = "Merchant name, once extracted", example = "Shoprite")
        String merchantName,
        @Schema(description = "Total amount, once extracted", example = "12500.00")
        BigDecimal totalAmount,
        @Schema(description = "ISO currency code", example = "NGN")
        String currency,
        @Schema(description = "Date on the receipt, once extracted", example = "2026-06-15")
        LocalDate receiptDate,
        @Schema(description = "When the receipt was uploaded")
        Instant createdAt,
        @Schema(description = "Extracted line items; empty until processing completes")
        List<ReceiptItemDTO> items,
        @Schema(description = "Processing status: PENDING → PROCESSING → COMPLETED or FAILED")
        ProcessingStatus status,
        @Schema(description = "Human-readable failure reason; set only when status is FAILED",
                example = "The uploaded file does not appear to be a receipt.")
        String errorMessage
) {}
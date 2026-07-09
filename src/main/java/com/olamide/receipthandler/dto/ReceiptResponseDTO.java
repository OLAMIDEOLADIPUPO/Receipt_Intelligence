package com.olamide.receipthandler.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiptResponseDTO(
        UUID id,
        String merchantName,
        BigDecimal totalAmount,
        String currency,
        com.olamide.receipthandler.enums.Category category,
        LocalDate receiptDate,
        Instant createdAt
) {
}

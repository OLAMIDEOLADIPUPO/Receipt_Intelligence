package com.olamide.receipthandler.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReceiptResponseDTO(
        UUID id,
        String merchantName,
        BigDecimal totalAmount,
        String currency,
        LocalDate receiptDate,
        Instant createdAt,
        List<ReceiptItemDTO> items
) {}

package com.olamide.receipthandler.dto;

import com.olamide.receipthandler.enums.Category;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiptItemWithContextDTO(
        UUID itemId,
        String name,
        BigDecimal amount,
        Category category,
        UUID receiptId,
        String merchantName,
        LocalDate receiptDate
) {}
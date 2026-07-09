package com.olamide.receipthandler.dto;


import com.olamide.receipthandler.enums.Category;
import java.math.BigDecimal;
import java.util.UUID;

public record ReceiptItemDTO(
        UUID id,
        String name,
        BigDecimal amount,
        Category category
) {}
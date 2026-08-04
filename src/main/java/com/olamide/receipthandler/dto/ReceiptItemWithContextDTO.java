package com.olamide.receipthandler.dto;

import com.olamide.receipthandler.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "A line item with its parent receipt's merchant and date attached. Used for category-based queries.")
public record ReceiptItemWithContextDTO(
        @Schema(description = "Item ID") UUID itemId,
        @Schema(description = "Item name", example = "Eggs (dozen)") String name,
        @Schema(description = "Item amount", example = "2800.00") BigDecimal amount,
        @Schema(description = "Category") Category category,
        @Schema(description = "Parent receipt ID") UUID receiptId,
        @Schema(description = "Parent receipt's merchant", example = "Shoprite") String merchantName,
        @Schema(description = "Parent receipt's date", example = "2026-06-15") LocalDate receiptDate
) {}
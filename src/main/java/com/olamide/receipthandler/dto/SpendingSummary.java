package com.olamide.receipthandler.dto;

import com.olamide.receipthandler.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Spending totals for a month, broken down by category, with a separate bucket for undated receipts.")
public record SpendingSummary(
        @Schema(description = "Total spend for the period", example = "84500.00") BigDecimal totalSpend,
        @Schema(description = "ISO currency code", example = "NGN") String currency,
        @Schema(description = "Human-readable period label", example = "JUNE 2026") String period,
        @Schema(description = "Per-category breakdown") List<CategoryBreakdown> breakdown,
        @Schema(description = "Total from receipts whose date couldn't be determined", example = "5000.00") BigDecimal unknownDateTotal,
        @Schema(description = "Number of receipts with an unknown date", example = "2") int unknownDateCount
) {
    @Schema(description = "Spending within a single category")
    public record CategoryBreakdown(
            @Schema(description = "Category") Category category,
            @Schema(description = "Total amount in this category", example = "32000.00") BigDecimal amount,
            @Schema(description = "Number of items in this category", example = "14") int count
    ){}
}

package com.olamide.receipthandler.dto;

import com.olamide.receipthandler.enums.Category;

import java.math.BigDecimal;
import java.util.List;

public record SpendingSummary(
        BigDecimal totalSpend,
        String currency,
        String period,
        List<CategoryBreakdown>breakdown,
        BigDecimal unknownDateTotal,
        int unknownDateCount
) {
    public record CategoryBreakdown(
            Category category,
            BigDecimal amount,
            int count
    ){}
}

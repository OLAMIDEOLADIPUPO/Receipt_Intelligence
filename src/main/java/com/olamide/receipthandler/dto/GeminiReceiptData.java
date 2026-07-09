package com.olamide.receipthandler.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GeminiReceiptData(
        Boolean isReceipt,
        String merchantName,
        BigDecimal totalAmount,
        LocalDate receiptDate,
        List<GeminiLineItem> items

) {
    public record GeminiLineItem(
            String name,
            BigDecimal amount,
            String category
    ) {}
}

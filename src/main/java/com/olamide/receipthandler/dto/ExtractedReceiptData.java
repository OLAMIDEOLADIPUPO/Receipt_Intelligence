package com.olamide.receipthandler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public record ExtractedReceiptData(
        Boolean isReceipt,
        String merchantName,
        BigDecimal totalAmount,
        LocalDate receiptDate,
        List<ExtractedLineItem> items
) {
    public record ExtractedLineItem(String name, BigDecimal amount, String category) {}
}

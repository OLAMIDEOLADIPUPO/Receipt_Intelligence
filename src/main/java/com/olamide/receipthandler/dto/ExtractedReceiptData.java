package com.olamide.receipthandler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Structured receipt data as parsed from any AI provider's response.
 * Replaces the old provider-named {@code GeminiReceiptData} — this shape
 * is the shared contract every {@code ReceiptExtractionService} implementation
 * must produce, regardless of which model actually did the reading.
 */
public record ExtractedReceiptData(
        Boolean isReceipt,
        String merchantName,
        BigDecimal totalAmount,
        LocalDate receiptDate,
        List<ExtractedLineItem> items
) {
    public record ExtractedLineItem(String name, BigDecimal amount, String category) {}
}

package com.olamide.receipthandler.dto;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GeminiReceiptData(
        Boolean isReceipt,
        String merchantName,
        BigDecimal totalAmount,
        String category,
        LocalDate receiptDate

) {
}

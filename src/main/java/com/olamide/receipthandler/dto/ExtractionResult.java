package com.olamide.receipthandler.dto;

/**
 * Result of a {@code ReceiptExtractionService.extractReceiptData} call.
 * Replaces the old provider-named {@code GeminiAnalysisResult}.
 *
 * @param extractedReceiptData the parsed, structured data
 * @param rawResponse          the raw text the provider returned, stored on the
 *                              Receipt entity for debugging when parsing goes wrong
 */
public record ExtractionResult(
        ExtractedReceiptData extractedReceiptData,
        String rawResponse
) {}

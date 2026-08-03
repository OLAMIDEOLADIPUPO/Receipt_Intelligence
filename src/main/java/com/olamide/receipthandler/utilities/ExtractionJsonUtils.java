package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.ExtractedReceiptData;
import com.olamide.receipthandler.exceptions.ExtractionParseException;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared defensive-parsing logic for turning a raw AI provider text response
 * into {@link ExtractedReceiptData}. Every provider is prompted to return
 * bare JSON, but models occasionally wrap it in markdown backticks or add
 * a stray sentence before/after — this cleans both cases the same way for
 * every provider, so a fix here benefits all of them at once.
 */
public final class ExtractionJsonUtils {

    private ExtractionJsonUtils() {}

    static String stripToJsonObject(String rawText) {
        String cleaned = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start != -1 && end != -1) {
            cleaned = cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    public static ExtractedReceiptData parse(ObjectMapper objectMapper, String rawText, String providerName) {
        String cleanJson = stripToJsonObject(rawText);
        try {
            return objectMapper.readValue(cleanJson, ExtractedReceiptData.class);
        } catch (Exception e) {
            throw new ExtractionParseException("Could not parse " + providerName + " response: " + e.getMessage());
        }
    }
}

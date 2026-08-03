package com.olamide.receipthandler.exceptions;

/**
 * Thrown when an AI provider's response can't be parsed into structured
 * receipt data — malformed JSON, unexpected shape, etc. Provider-neutral:
 * replaces the old Gemini-specific {@code GeminiParseException} now that
 * multiple providers (Gemini, Claude) can throw it.
 */
public class ExtractionParseException extends RuntimeException {
    public ExtractionParseException(String message) {
        super(message);
    }
}

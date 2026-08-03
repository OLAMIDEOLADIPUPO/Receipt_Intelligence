package com.olamide.receipthandler.exceptions;

/**
 * Thrown when a call to an AI provider itself fails — network error, rate
 * limit exhausted after retries, non-2xx response, unexpected response
 * shape. Provider-neutral: replaces the old Gemini-specific
 * {@code GeminiServiceException} now that multiple providers (Gemini, Claude)
 * can throw it.
 */
public class ExtractionServiceException extends RuntimeException {
    public ExtractionServiceException(String message) {
        super(message);
    }
}

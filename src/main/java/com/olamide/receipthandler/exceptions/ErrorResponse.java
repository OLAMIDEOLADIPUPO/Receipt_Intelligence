package com.olamide.receipthandler.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standard error response returned for 4xx/5xx failures")
public record ErrorResponse(
        @Schema(description = "Machine-readable error code", example = "INVALID_FILE_FORMAT")
        String error,
        @Schema(description = "Human-readable error message", example = "Please upload a JPEG, PNG, WEBP, or PDF file.")
        String message,
        @Schema(description = "When the error occurred")
        LocalDateTime timestamp
) {
}

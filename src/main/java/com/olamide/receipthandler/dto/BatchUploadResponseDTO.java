package com.olamide.receipthandler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Outcome of a batch upload: accepted receipts and rejected files, reported separately.")
public record BatchUploadResponseDTO(
        @Schema(description = "Receipts accepted and queued for processing")
        List<ReceiptResponseDTO> accepted,
        @Schema(description = "Files that failed validation, each with the reason")
        List<BatchFileError> rejected
) {
    @Schema(description = "A file that was rejected during batch upload")
    public record BatchFileError(
            @Schema(description = "Original filename", example = "blurry-scan.png") String fileName,
            @Schema(description = "Why the file was rejected", example = "Please upload a JPEG, PNG, WEBP, or PDF file.") String reason) {}
}
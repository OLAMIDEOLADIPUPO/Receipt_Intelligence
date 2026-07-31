package com.olamide.receipthandler.dto;

import java.util.List;

public record BatchUploadResponseDTO(
        List<ReceiptResponseDTO> accepted,
        List<BatchFileError> rejected
) {
    public record BatchFileError(String fileName, String reason) {}
}
package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.ExtractionResult;

public interface ReceiptExtractionService {

    /**
     * @param fileBytes raw bytes of the uploaded receipt (image or PDF)
     * @param mimeType  content type of the file, e.g. "image/jpeg", "application/pdf"
     * @return the parsed receipt data plus the raw provider response (kept for debugging)
     * @throws com.olamide.receipthandler.exceptions.ExtractionServiceException if the provider call itself fails
     * @throws com.olamide.receipthandler.exceptions.ExtractionParseException   if the provider's response can't be parsed into JSON
     */
    ExtractionResult extractReceiptData(byte[] fileBytes, String mimeType);
}

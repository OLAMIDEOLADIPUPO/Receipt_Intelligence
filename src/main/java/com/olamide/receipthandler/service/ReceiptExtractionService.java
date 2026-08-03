package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.ExtractionResult;

/**
 * Provider-agnostic contract for pulling structured receipt data out of an
 * uploaded file (image or PDF). Implementations own everything about how
 * they talk to their underlying AI provider — the rest of the app only
 * ever depends on this interface.
 *
 * To add a new provider: implement this interface, then wire it into
 * {@code ExtractionConfig} behind the {@code receipt.extraction.provider}
 * property. Nothing else needs to change.
 */
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

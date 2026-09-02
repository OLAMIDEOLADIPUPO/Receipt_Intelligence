package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.ExtractionResult;

public interface ReceiptExtractionService {


    ExtractionResult extractReceiptData(byte[] fileBytes, String mimeType);
}

package com.olamide.receipthandler.service.async;

import com.olamide.receipthandler.dto.GeminiAnalysisResult;
import com.olamide.receipthandler.dto.GeminiReceiptData;
import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.enums.ProcessingStatus;
import com.olamide.receipthandler.models.Receipt;
import com.olamide.receipthandler.models.ReceiptItem;
import com.olamide.receipthandler.repository.ReceiptItemRepository;
import com.olamide.receipthandler.repository.ReceiptRepository;
import com.olamide.receipthandler.service.GeminiClient;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReceiptAsyncProcessor {
    private static final Logger log = LoggerFactory.getLogger(ReceiptAsyncProcessor.class);
    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final GeminiClient geminiClient;

    public ReceiptAsyncProcessor(ReceiptRepository receiptRepository, ReceiptItemRepository receiptItemRepository, GeminiClient geminiClient) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.geminiClient = geminiClient;
    }

    @Async("geminiTaskExecutor")
    @Transactional
    public void processReceiptAsync(UUID receiptId, byte[] fileBytes,String mimeType) {
        Receipt receipt = receiptRepository.findById(receiptId).orElse(null);
        if (receipt == null) {
            log.error("Async processing triggered for missing receipt id={}", receiptId);
            return;
        }

        try{
            receipt.setStatus(ProcessingStatus.PROCESSING);
            receiptRepository.save(receipt);

            GeminiAnalysisResult result = geminiClient.analyzeReceipt(fileBytes, mimeType);
            GeminiReceiptData receiptData = result.geminiReceiptData();

            if (!Boolean.TRUE.equals(receiptData.isReceipt())) {
                failReceipt(receipt, "The uploaded file does not appear to be a receipt.");
                return;
            }
            if (receiptData.totalAmount() == null && receiptData.merchantName() == null) {
                failReceipt(receipt, "Receipt was too blurry to read. Please upload a clearer image.");
                return;
            }
            receipt.setMerchantName(receiptData.merchantName());
            receipt.setTotalAmount(receiptData.totalAmount());
            receipt.setDate(receiptData.receiptDate());
            receipt.setGeminiRawResponse(result.rawResponse());
            receipt.setStatus(ProcessingStatus.COMPLETED);
            Receipt saved = receiptRepository.save(receipt);

            if (receiptData.items() != null && !receiptData.items().isEmpty()) {
                List<ReceiptItem> items = receiptData.items().stream()
                        .map(item -> {
                            Category category = Category.fromString(item.category());
                            return new ReceiptItem(saved, item.name(), item.amount(), category);
                        })
                        .collect(Collectors.toList());
                receiptItemRepository.saveAll(items);
            }

        }catch (Exception e) {
        log.error("Gemini processing failed for receipt id={}", receiptId, e);
        failReceipt(receipt, "Processing failed: " + e.getMessage());

        }

    }
    private void failReceipt(Receipt receipt, String message) {
        receipt.setStatus(ProcessingStatus.FAILED);
        receipt.setErrorMessage(message);
        receiptRepository.save(receipt);
    }
    
}

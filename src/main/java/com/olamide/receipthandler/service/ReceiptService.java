package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.ReceiptItemWithContextDTO;
import com.olamide.receipthandler.dto.ReceiptResponseDTO;
import com.olamide.receipthandler.dto.SpendingSummary;
import com.olamide.receipthandler.enums.Category;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface ReceiptService {
    ReceiptResponseDTO processReceipt(MultipartFile file);
    List<ReceiptResponseDTO> getAllReceipts();
    SpendingSummary getSpendingSummary(YearMonth yearMonth);
    ReceiptResponseDTO getReceiptById(UUID id);
    List<ReceiptItemWithContextDTO> getItemsByCategory(Category category);
}
package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.ReceiptResponseDTO;
import com.olamide.receipthandler.dto.SpendingSummary;
import com.olamide.receipthandler.enums.Category;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptService {
    ReceiptResponseDTO processReceipt(MultipartFile file);
    List<ReceiptResponseDTO> getAllReceipts(Category category);
    SpendingSummary getSpendingSummary(YearMonth yearMonth);
    ReceiptResponseDTO getReceiptById(UUID id);



}

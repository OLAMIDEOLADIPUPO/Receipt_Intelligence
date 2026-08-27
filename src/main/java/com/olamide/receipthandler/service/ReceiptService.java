package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.*;
import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.models.Staff;
import com.olamide.receipthandler.models.User;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface ReceiptService {
    ReceiptResponseDTO processReceipt(MultipartFile file);

    BatchUploadResponseDTO processBatch(UUID staffId, List<MultipartFile> files);

    // Public self-upload path (no login). The caller (controller) has already
    // resolved `staff` from the roster via employeeId and supplies the fixed
    // placeholder system `user` these receipts attach to, since Receipt.user
    // is NOT NULL and there is no authenticated User in this flow. Mirrors
    // processBatch's per-file validation/accept/reject behavior exactly.
    BatchUploadResponseDTO processSelfUpload(User systemUser, Staff staff, List<MultipartFile> files);

    PagedResponse<ReceiptResponseDTO> getAllReceipts(YearMonth month, int page, int size);
    SpendingSummary getSpendingSummary(YearMonth yearMonth);
    ReceiptResponseDTO getReceiptById(UUID id);
    PagedResponse<ReceiptItemWithContextDTO> getItemsByCategory(Category category, int page, int size);
}
package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.models.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, UUID> {
    List<ReceiptItem> findByReceiptId(UUID receiptId);
}
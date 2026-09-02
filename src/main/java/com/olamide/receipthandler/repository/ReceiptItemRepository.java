package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.models.ReceiptItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, UUID> {

    List<ReceiptItem> findByReceiptId(UUID receiptId);
    List<ReceiptItem> findByReceiptIdIn(List<UUID> receiptIds);


    @Query(value = """
        SELECT i FROM ReceiptItem i
        JOIN FETCH i.receipt r
        WHERE i.category = :category
        ORDER BY r.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(i) FROM ReceiptItem i
        WHERE i.category = :category
        """)
    Page<ReceiptItem> findByCategory(
            @Param("category") Category category,
            Pageable pageable
    );
}
package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.models.ReceiptItem;
import com.olamide.receipthandler.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, UUID> {

    List<ReceiptItem> findByReceiptId(UUID receiptId);
    List<ReceiptItem> findByReceiptIdIn(List<UUID> receiptIds);

    // Items belonging to the given user, filtered by category.
    // Joins through Receipt because ReceiptItem has no direct User reference.
    @Query("""
        SELECT i FROM ReceiptItem i
        JOIN FETCH i.receipt r
        WHERE r.user = :user
        AND i.category = :category
        ORDER BY r.createdAt DESC
        """)
    List<ReceiptItem> findByUserAndCategory(
            @Param("user") User user,
            @Param("category") Category category
    );
}
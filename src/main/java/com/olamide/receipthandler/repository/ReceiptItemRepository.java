package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.models.ReceiptItem;
import com.olamide.receipthandler.models.User;
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

    // Items belonging to the given user, filtered by category, newest receipt first.
    // Joins through Receipt because ReceiptItem has no direct User reference.
    // JOIN FETCH is a to-one fetch (item -> receipt), so it pages safely in the DB
    // with no in-memory-paging penalty. countQuery is supplied explicitly because
    // Hibernate can't derive a count from a fetch-join query.
    @Query(value = """
        SELECT i FROM ReceiptItem i
        JOIN FETCH i.receipt r
        WHERE r.user = :user
        AND i.category = :category
        ORDER BY r.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(i) FROM ReceiptItem i
        WHERE i.receipt.user = :user
        AND i.category = :category
        """)
    Page<ReceiptItem> findByUserAndCategory(
            @Param("user") User user,
            @Param("category") Category category,
            Pageable pageable
    );
}
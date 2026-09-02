package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.enums.ProcessingStatus;
import com.olamide.receipthandler.models.Receipt;
import com.olamide.receipthandler.models.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findByDateIsNull();

    Page<Receipt> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
        SELECT r FROM Receipt r
        WHERE r.date BETWEEN :start AND :end
        ORDER BY r.createdAt DESC
        """)
    List<Receipt> findByDateBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Paged variant used by the receipts list when a month filter is applied.
    // Matches the export's date-window semantics so the table and the Excel
    // download cover exactly the same receipts.
    @Query("""
        SELECT r FROM Receipt r
        WHERE r.date BETWEEN :start AND :end
        ORDER BY r.createdAt DESC
        """)
    Page<Receipt> findByDateBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            Pageable pageable
    );


    @Query(value = """
        SELECT DISTINCT r FROM Receipt r
        JOIN r.items i
        WHERE i.category = :category
        ORDER BY r.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT r) FROM Receipt r
        JOIN r.items i
        WHERE i.category = :category
        """)
    Page<Receipt> findByItemsCategory(
            @Param("category") Category category,
            Pageable pageable
    );

    @Query(value = """
        SELECT DISTINCT r FROM Receipt r
        JOIN r.items i
        WHERE i.category = :category
        AND r.date BETWEEN :start AND :end
        ORDER BY r.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT r) FROM Receipt r
        JOIN r.items i
        WHERE i.category = :category
        AND r.date BETWEEN :start AND :end
        """)
    Page<Receipt> findByItemsCategoryAndDateBetween(
            @Param("category") Category category,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            Pageable pageable
    );

    List<Receipt> findByStaffOrderByCreatedAtDesc(Staff staff);

    @Query("""
        SELECT r FROM Receipt r
        WHERE r.staff = :staff
        AND r.date BETWEEN :start AND :end
        ORDER BY r.createdAt DESC
        """)
    List<Receipt> findByStaffAndDateBetween(
            @Param("staff") Staff staff,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT r FROM Receipt r
    LEFT JOIN FETCH r.staff s
    WHERE r.status = :status
    AND r.date BETWEEN :start AND :end
    ORDER BY s.name ASC NULLS LAST, r.date ASC
    """)
    List<Receipt> findForExport(
            @Param("status") ProcessingStatus status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /**
     * Check if a staff member has already submitted receipts within a given
     * window. Used by the self-upload endpoint to prevent duplicate monthly
     * submissions
     */
    @Query("""
        SELECT COUNT(r) > 0 FROM Receipt r
        WHERE r.staff = :staff
        AND r.createdAt >= :start
        AND r.createdAt < :end
        """)
    boolean existsByStaffAndCreatedAtBetween(
            @Param("staff") Staff staff,
            @Param("start") Instant start,
            @Param("end") Instant end
    );


    @Query("""
        SELECT r FROM Receipt r
        WHERE r.status = :status
        AND COALESCE(r.updatedAt, r.createdAt) < :cutoff
        """)
    List<Receipt> findStuckInStatus(
            @Param("status") ProcessingStatus status,
            @Param("cutoff") Instant cutoff
    );
}
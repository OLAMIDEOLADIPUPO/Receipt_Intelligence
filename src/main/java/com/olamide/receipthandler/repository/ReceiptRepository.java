package com.olamide.receipthandler.repository;

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

    // These read paths are intentionally NOT scoped to a User: Accounts uses
    // them to review/export every staff member's receipts company-wide,
    // regardless of whether a receipt was uploaded by a logged-in Accounts
    // user or via the public staff self-upload flow (which attaches to the
    // fixed placeholder system user).

    // Receipts with no extracted date — used by the spending summary's
    // "unknown date" bucket.
    List<Receipt> findByDateIsNull();

    // Newest-first page of every receipt — used by the receipts list when no
    // month filter is applied.
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
     * Check if a staff member has already uploaded receipts for a given month.
     * Used by the self-upload endpoint to prevent duplicate monthly submissions.
     */
    @Query("""
        SELECT COUNT(r) > 0 FROM Receipt r
        WHERE r.staff = :staff
        AND r.date BETWEEN :start AND :end
        """)
    boolean existsByStaffAndDateBetween(
            @Param("staff") Staff staff,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Rows stuck mid-processing: still PROCESSING but their last write is older
    // than the cutoff. updatedAt marks when the PROCESSING transition was
    // persisted; for rows that predate the updatedAt column it's null, so we
    // fall back to createdAt. Either timestamp older than the cutoff => stuck.
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
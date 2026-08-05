package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.enums.ProcessingStatus;
import com.olamide.receipthandler.models.Receipt;
import com.olamide.receipthandler.models.Staff;
import com.olamide.receipthandler.models.User;
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

    List<Receipt> findByUserAndDateIsNull(User user);
    Page<Receipt> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Optional<Receipt> findByIdAndUser(UUID id, User user);

    @Query("""
        SELECT r FROM Receipt r
        WHERE r.user = :user
        AND r.date BETWEEN :start AND :end
        ORDER BY r.createdAt DESC
        """)
    List<Receipt> findByUserAndDateBetween(
            @Param("user") User user,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Paged variant used by the receipts list when a month filter is applied.
    // Matches the export's date-window semantics so the table and the Excel
    // download cover exactly the same receipts.
    @Query("""
        SELECT r FROM Receipt r
        WHERE r.user = :user
        AND r.date BETWEEN :start AND :end
        ORDER BY r.createdAt DESC
        """)
    Page<Receipt> findByUserAndDateBetween(
            @Param("user") User user,
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
    WHERE r.user = :user
    AND r.status = :status
    AND r.date BETWEEN :start AND :end
    ORDER BY s.name ASC NULLS LAST, r.date ASC
    """)
    List<Receipt> findForExport(
            @Param("user") User user,
            @Param("status") ProcessingStatus status,
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
package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.models.Receipt;
import com.olamide.receipthandler.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    List<Receipt> findByUserAndDateIsNull(User user);
    List<Receipt> findByUserOrderByCreatedAtDesc(User user);
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
}
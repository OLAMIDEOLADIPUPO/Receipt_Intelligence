package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.models.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findByCategoryOrderByCreatedAtDesc(Category category);
    List<Receipt> findByDateBetween(LocalDate start, LocalDate end);
    List<Receipt> findAllByOrderByCreatedAtDesc();
    List<Receipt> findByDateIsNull();


}

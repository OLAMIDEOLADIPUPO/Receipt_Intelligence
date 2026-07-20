package com.olamide.receipthandler.models;

import com.olamide.receipthandler.enums.Category;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "receipt_items", indexes = {
        @Index(name = "idx_receiptitem_receipt_id", columnList = "receipt_id"),
        @Index(name = "idx_receiptitem_category", columnList = "category")
})

public class ReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    protected ReceiptItem() {}

    public ReceiptItem(Receipt receipt, String name, BigDecimal amount, Category category) {
        this.receipt = receipt;
        this.name = name;
        this.amount = amount;
        this.category = category;
    }

    public UUID getId() { return id; }
    public Receipt getReceipt() { return receipt; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public Category getCategory() { return category; }
}
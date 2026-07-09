package com.olamide.receipthandler.models;

import com.olamide.receipthandler.enums.Category;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "receipt")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = true)
    private String merchantName;

    private String currency;

    @Column(nullable = true)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category = Category.OTHER;

    private LocalDate date;

    @Column(nullable = true,columnDefinition = "TEXT")
    private String geminiRawResponse;

    @Column(nullable = true)
    private String rawImagePath;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private Instant createdAt;

    protected Receipt() {}
    public Receipt(String merchantName, String currency, BigDecimal totalAmount, Category category, LocalDate date, String rawImagePath, String geminiRawResponse) {
        this.merchantName = merchantName;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.category = category;
        this.date = date;
        this.rawImagePath = rawImagePath;
        this.geminiRawResponse = geminiRawResponse;

    }

    public UUID getId() {
        return id;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Category getCategory() {
        return category;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getRawImagePath() {
        return rawImagePath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}

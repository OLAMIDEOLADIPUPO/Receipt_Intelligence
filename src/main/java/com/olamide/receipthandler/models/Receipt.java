package com.olamide.receipthandler.models;

import com.olamide.receipthandler.enums.Category;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "receipt")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = true)
    private String merchantName;

    private String currency;

    @Column(nullable = true)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReceiptItem> items = new ArrayList<>();

    private LocalDate date;

    @Column(nullable = true,columnDefinition = "TEXT")
    private String geminiRawResponse;

    @Column(nullable = true)
    private String rawImagePath;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private Instant createdAt;

    protected Receipt() {}
    public Receipt(User user,String merchantName, String currency, BigDecimal totalAmount,  LocalDate date, String rawImagePath, String geminiRawResponse) {
        this.user = user;
        this.merchantName = merchantName;
        this.currency = currency;
        this.totalAmount = totalAmount;
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

    public User getUser() {
        return user;
    }

    public String getGeminiRawResponse() {
        return geminiRawResponse;
    }

    public List<ReceiptItem> getItems() {
        return items;
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

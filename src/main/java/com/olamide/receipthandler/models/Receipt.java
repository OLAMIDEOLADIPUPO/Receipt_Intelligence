package com.olamide.receipthandler.models;


import com.olamide.receipthandler.enums.ProcessingStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "receipt", indexes = {
        @Index(name = "idx_receipt_user_id", columnList = "user_id"),
        @Index(name = "idx_receipt_date", columnList = "date"),
        @Index(name = "idx_receipt_created_at", columnList = "createdAt"),
        @Index(name = "idx_receipt_user_date", columnList = "user_id, date"),
        @Index(name = "idx_receipt_staff_id", columnList = "staff_id")
})
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = true)
    private Staff staff;

    @Column(nullable = true)
    private String merchantName;

    private String currency;

    @Column(nullable = true)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReceiptItem> items = new ArrayList<>();

    private LocalDate date;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String geminiRawResponse;

    @Column(nullable = true)
    private String rawImagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'COMPLETED'")
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Updated on every persist. For a row stuck in PROCESSING, the last write
    // before a crash was the PROCESSING transition, so this marks when
    // processing began — the StuckReceiptReaperJob uses it to detect staleness.
    // Nullable so ddl-auto can add the column to pre-existing rows; the reaper
    // falls back to createdAt for rows that predate this field.
    @UpdateTimestamp
    @Column(nullable = true)
    private Instant updatedAt;

    protected Receipt() {}

    // Original constructor kept in case anything else constructs a fully-formed
    // Receipt in one shot. Not used by the async upload path anymore.
    public Receipt(User user, String merchantName, String currency, BigDecimal totalAmount,
                   LocalDate date, String rawImagePath, String geminiRawResponse) {
        this.user = user;
        this.merchantName = merchantName;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.date = date;
        this.rawImagePath = rawImagePath;
        this.geminiRawResponse = geminiRawResponse;
        this.status = ProcessingStatus.COMPLETED;
    }

    // Existing placeholder constructor for the async flow. Unchanged —
    // still used by the current single-file processReceipt() path, which
    // doesn't tag a staff member. staff stays null for receipts created
    // this way.
    public Receipt(User user, String currency) {
        this.user = user;
        this.currency = currency;
    }

    // New: placeholder constructor for the staff-tagged batch upload flow
    // (task 5). Same async-fill-in-later pattern as above, plus staff set
    // at creation time since the batch flow collects it before upload.
    public Receipt(User user, String currency, Staff staff) {
        this.user = user;
        this.currency = currency;
        this.staff = staff;
    }

    public UUID getId() { return id; }
    public String getMerchantName() { return merchantName; }
    public String getCurrency() { return currency; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public User getUser() { return user; }
    public Staff getStaff() { return staff; }

    public List<ReceiptItem> getItems() { return items; }
    public LocalDate getDate() { return date; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public ProcessingStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }

    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setGeminiRawResponse(String geminiRawResponse) { this.geminiRawResponse = geminiRawResponse; }
    public void setStatus(ProcessingStatus status) { this.status = status; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setStaff(Staff staff) { this.staff = staff; }
}
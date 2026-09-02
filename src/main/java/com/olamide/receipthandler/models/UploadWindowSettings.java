package com.olamide.receipthandler.models;

import com.olamide.receipthandler.enums.OverrideMode;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Singleton row (fixed id) holding Accounts' manual override for the staff
 * self-upload window. Always fetched/created via UploadWindowServiceImpl —
 * never construct or query this outside that service.
 */
@Entity
@Table(name = "upload_window_settings")
public class UploadWindowSettings {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OverrideMode overrideMode = OverrideMode.AUTO;

    @UpdateTimestamp
    @Column(nullable = true)
    private Instant updatedAt;

    protected UploadWindowSettings() {}

    public UploadWindowSettings(OverrideMode overrideMode) {
        this.id = SINGLETON_ID;
        this.overrideMode = overrideMode;
    }

    public Long getId() { return id; }
    public OverrideMode getOverrideMode() { return overrideMode; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setOverrideMode(OverrideMode overrideMode) { this.overrideMode = overrideMode; }
}

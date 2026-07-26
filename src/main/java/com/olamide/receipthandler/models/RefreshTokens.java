package com.olamide.receipthandler.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refreshtoken_user_id", columnList = "user_id"),
        @Index(name = "idx_refreshtoken_expires_at", columnList = "expiresAt")
})
public class RefreshTokens {

    @Id
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean revoked;

    private String replacedByJti;

    protected RefreshTokens() {}

    public RefreshTokens(String jti, User user, Instant expiresAt) {
        this.jti = jti;
        this.user = user;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.revoked = false;
    }

    public String getJti() { return jti; }
    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRevoked() { return revoked; }
    public String getReplacedByJti() { return replacedByJti; }

    public void revoke() { this.revoked = true; }
    public void setReplacedByJti(String replacedByJti) { this.replacedByJti = replacedByJti; }
}
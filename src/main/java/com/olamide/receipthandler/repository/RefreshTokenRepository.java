package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.models.RefreshTokens;
import com.olamide.receipthandler.models.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokens, String> {

    Optional<RefreshTokens> findByJti(String jti);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshTokens r WHERE r.jti = :jti")
    Optional<RefreshTokens> findByJtiForUpdate(@Param("jti") String jti);

    @Modifying
    @Query("UPDATE RefreshTokens r SET r.revoked = true WHERE r.user = :user AND r.revoked = false")
    void revokeAllActiveTokensForUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM RefreshTokens r WHERE r.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}
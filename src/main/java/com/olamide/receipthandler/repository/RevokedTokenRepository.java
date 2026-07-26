package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.models.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Date;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    boolean existsByJti(String jti);

    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expiresAt<:now")
    void deleteExpiredTokens(@Param("now") Instant now);
}

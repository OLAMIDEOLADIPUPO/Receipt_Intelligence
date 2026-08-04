package com.olamide.receipthandler.components;

import com.olamide.receipthandler.repository.RefreshTokenRepository;
import com.olamide.receipthandler.repository.RevokedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;


@Component
public class RevokedTokenCleanUpJob {

    private final RevokedTokenRepository revokedTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RevokedTokenCleanUpJob(RevokedTokenRepository revokedTokenRepository,
                                  RefreshTokenRepository refreshTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(fixedRateString = "${app.jobs.revoked-token-cleanup.rate}")
    @Transactional
    public void cleanup() {
        revokedTokenRepository.deleteExpiredTokens(Instant.now());
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }
}
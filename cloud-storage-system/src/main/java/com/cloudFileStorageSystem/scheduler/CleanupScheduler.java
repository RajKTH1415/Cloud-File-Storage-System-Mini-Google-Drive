package com.cloudFileStorageSystem.scheduler;

import com.cloudFileStorageSystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final OtpRepository otpRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredData() {

        log.info("Cleanup Job Started");

        refreshTokenRepository
                .deleteByExpiryDateBefore(
                        new Date()
                );

        refreshTokenRepository
                .deleteByRevokedTrue();

        tokenBlacklistRepository
                .deleteByExpiryDateBefore(
                        new Date()
                );

        otpRepository
                .deleteByExpiryTimeBefore(
                        LocalDateTime.now()
                );

        log.info("Cleanup Job Completed");
    }
}
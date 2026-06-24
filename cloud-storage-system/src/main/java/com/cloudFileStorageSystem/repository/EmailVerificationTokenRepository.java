package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.module.EmailVerificationToken;
import com.cloudFileStorageSystem.module.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    long countByUserAndCreatedAtAfter(Users user, LocalDateTime localDateTime);

    Optional<EmailVerificationToken> findTopByUserOrderByCreatedAtDesc(Users user);

    void deleteByUser(Users user);
}
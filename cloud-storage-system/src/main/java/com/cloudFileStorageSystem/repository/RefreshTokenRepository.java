package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.module.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);


    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    void deleteByExpiryDateBefore(Date now);

    void deleteByRevokedTrue();
}

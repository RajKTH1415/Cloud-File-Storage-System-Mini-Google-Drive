package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.module.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByJti(String jti);

    void deleteByExpiryDateBefore(Date date);
}

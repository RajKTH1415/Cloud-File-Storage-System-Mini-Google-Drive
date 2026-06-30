package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.module.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {


    List<PasswordHistory> findTop5ByUserIdOrderByChangedAtDesc(String userId);
}

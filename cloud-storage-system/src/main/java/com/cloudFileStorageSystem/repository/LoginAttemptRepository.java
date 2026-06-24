package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.module.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    List<LoginAttempt> findByUserIdOrderByAttemptTimeDesc(String userId);
}

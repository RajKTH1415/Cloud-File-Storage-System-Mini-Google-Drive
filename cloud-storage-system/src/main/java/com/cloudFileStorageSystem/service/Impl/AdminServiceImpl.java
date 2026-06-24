package com.cloudFileStorageSystem.service.Impl;



import com.cloudFileStorageSystem.dtos.response.UnlockUserResponse;
import com.cloudFileStorageSystem.module.AuditLog;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.AuditLogRepository;
import com.cloudFileStorageSystem.repository.UsersRepository;
import com.cloudFileStorageSystem.service.AdminService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminServiceImpl implements AdminService {

    private final UsersRepository usersRepository;
    private final AuditLogRepository auditLogRepository;


    public AdminServiceImpl(
            UsersRepository usersRepository, AuditLogRepository auditLogRepository) {

        this.usersRepository = usersRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public UnlockUserResponse unlockUser(Long userId) {

        Users user =
                usersRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"));

        if (user.isAccountNonLocked()) {
            throw new RuntimeException(
                    "User account is already unlocked");
        }

        user.setAccountNonLocked(true);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);


        usersRepository.save(user);

        AuditLog auditLog = new AuditLog();

        auditLog.setIdentifier(user.getEmail());
        auditLog.setAction("ACCOUNT_UNLOCKED_BY_ADMIN");
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(
                "Account unlocked by administrator"
        );

        auditLogRepository.save(auditLog);

        return UnlockUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accountNonLocked(user.isAccountNonLocked())
                .failedAttempts(user.getFailedAttempts())
                .unlockedAt(LocalDateTime.now())
                .build();
    }
}

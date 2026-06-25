package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.dtos.response.UnlockUserResponse;
import com.cloudFileStorageSystem.dtos.response.UsersResponse;
import com.cloudFileStorageSystem.enums.Role;
import com.cloudFileStorageSystem.module.AuditLog;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.AuditLogRepository;
import com.cloudFileStorageSystem.repository.UsersRepository;
import com.cloudFileStorageSystem.service.AdminService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    private final UsersRepository usersRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminServiceImpl(
            UsersRepository usersRepository,
            AuditLogRepository auditLogRepository) {

        this.usersRepository = usersRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public UnlockUserResponse unlockUser(Long userId) {

        log.info("Unlock user request received for userId={}", userId);

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found. userId={}", userId);
                    return new RuntimeException("User not found");
                });

        if (user.isAccountNonLocked()) {

            log.warn(
                    "Unlock operation skipped. User already unlocked. userId={}, email={}",
                    user.getId(),
                    user.getEmail()
            );

            throw new RuntimeException(
                    "User account is already unlocked"
            );
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String adminUsername = authentication.getName();

        log.info("Admin '{}' is unlocking user '{}' (userId={})",
                adminUsername,
                user.getEmail(),
                user.getId());

        user.setAccountNonLocked(true);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);

        usersRepository.save(user);

        log.info(
                "User account unlocked successfully. userId={}, email={}",
                user.getId(),
                user.getEmail()
        );

        AuditLog auditLog = new AuditLog();
        auditLog.setIdentifier(user.getEmail());
        auditLog.setAction("ACCOUNT_UNLOCKED_BY_ADMIN");
        auditLog.setTimestamp(LocalDateTime.now());

        auditLog.setDetails(
                String.format(
                        "Admin '%s' unlocked account '%s' (User ID: %d)",
                        adminUsername,
                        user.getEmail(),
                        user.getId()
                )
        );

        auditLogRepository.save(auditLog);

        log.info(
                "Audit log saved for unlock operation. userId={}, admin={}",
                user.getId(),
                adminUsername
        );

        return UnlockUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accountNonLocked(user.isAccountNonLocked())
                .failedAttempts(user.getFailedAttempts())
                .unlockedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public List<UsersResponse> getAllUsers() {
        return usersRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    @Override
    public UsersResponse getUserById(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId));

        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UsersResponse updateUserRole(Long userId, Role role) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId));
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Default admin role cannot be modified");
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String currentAdmin = authentication.getName();
        if (user.getUsername().equals(currentAdmin)
                && role == Role.USER) {

            throw new IllegalArgumentException(
                    "You cannot remove your own admin role");
        }

        if (user.getRole() == role) {
            throw new IllegalArgumentException(
                    "User already has role: " + role);
        }

        user.setRole(role);

        Users updatedUser = usersRepository.save(user);

        return mapToUserResponse(updatedUser);

    }


    private UsersResponse mapToUserResponse(Users user) {

        return UsersResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .accountNonLocked(user.isAccountNonLocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
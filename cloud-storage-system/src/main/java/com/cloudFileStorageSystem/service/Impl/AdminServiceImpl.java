package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.dtos.response.AdminStatsResponse;
import com.cloudFileStorageSystem.dtos.response.LockUserResponse;
import com.cloudFileStorageSystem.dtos.response.UnlockUserResponse;
import com.cloudFileStorageSystem.dtos.response.UsersResponse;
import com.cloudFileStorageSystem.enums.Role;
import com.cloudFileStorageSystem.enums.UserStatus;
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
import java.util.Objects;

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

        log.info(
                "[UNLOCK_USER] Unlock process started. UserId={}",
                userId);

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> {

                    log.warn(
                            "[UNLOCK_USER] User not found. UserId={}",
                            userId);

                    return new RuntimeException("User not found");
                });

        log.debug(
                "[UNLOCK_USER] User located. UserId={}, Username={}, AccountNonLocked={}",
                user.getId(),
                user.getUsername(),
                user.isAccountNonLocked());

        if (user.isAccountNonLocked()) {

            log.warn(
                    "[UNLOCK_USER] Unlock operation skipped. User already unlocked. UserId={}, Email={}",
                    user.getId(),
                    user.getEmail());

            throw new RuntimeException(
                    "User account is already unlocked");
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String adminUsername =
                Objects.requireNonNull(authentication).getName();

        log.info(
                "[UNLOCK_USER] Admin initiated unlock operation. Admin={}, UserId={}, Email={}",
                adminUsername,
                user.getId(),
                user.getEmail());

        user.setAccountNonLocked(true);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);

        usersRepository.save(user);

        log.info(
                "[UNLOCK_USER] User account unlocked successfully. UserId={}, FailedAttempts={}",
                user.getId(),
                user.getFailedAttempts());

        AuditLog auditLog = new AuditLog();

        auditLog.setIdentifier(user.getEmail());
        auditLog.setAction("ACCOUNT_UNLOCKED_BY_ADMIN");
        auditLog.setTimestamp(LocalDateTime.now());

        auditLog.setDetails(
                String.format(
                        "Admin '%s' unlocked account '%s' (User ID: %d)",
                        adminUsername,
                        user.getEmail(),
                        user.getId()));

        auditLogRepository.save(auditLog);

        log.info(
                "[UNLOCK_USER] Audit log saved successfully. UserId={}, Admin={}",
                user.getId(),
                adminUsername);

        log.info(
                "[UNLOCK_USER] Unlock process completed successfully. UserId={}",
                user.getId());

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

        log.info("[GET_ALL_USERS] User retrieval process started.");

        List<Users> users =
                usersRepository.findAll();

        log.debug(
                "[GET_ALL_USERS] Retrieved {} user(s) from database.",
                users.size());

        List<UsersResponse> responses =
                users.stream()
                        .map(this::mapToUserResponse)
                        .toList();

        log.info(
                "[GET_ALL_USERS] User retrieval completed successfully. TotalUsers={}",
                responses.size());

        return responses;
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

        String currentAdmin = Objects.requireNonNull(authentication).getName();
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

    @Override
    @Transactional
    public LockUserResponse lockUser(Long userId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId));

        // Prevent locking default admin
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Default admin account cannot be locked");
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        Long currentAdminId =
                Long.parseLong(Objects.requireNonNull(authentication).getName());

        // Prevent self-lock
        if (user.getId().equals(currentAdminId)) {
            throw new IllegalArgumentException(
                    "You cannot lock your own account");
        }

        // Already locked
        if (!user.isAccountNonLocked()) {
            throw new IllegalArgumentException(
                    "User account is already locked");
        }

        user.setAccountNonLocked(false);

        // Admin lock = indefinite lock
        user.setLockedUntil(null);

        usersRepository.save(user);

        AuditLog auditLog = new AuditLog();

        auditLog.setIdentifier(user.getEmail());
        auditLog.setAction("ACCOUNT_LOCKED_BY_ADMIN");
        auditLog.setTimestamp(LocalDateTime.now());

        auditLog.setDetails(
                String.format(
                        "Admin '%s' locked account '%s' (User ID: %d)",
                        authentication.getName(),
                        user.getEmail(),
                        user.getId()
                )
        );

        auditLogRepository.save(auditLog);

        return LockUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accountNonLocked(false)
                .lockedUntil(user.getLockedUntil())
                .lockedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public UsersResponse enableUser(Long userId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId));

        // Default admin protection
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Default admin account is always enabled");
        }

        if (user.isEnabled()) {
            throw new IllegalArgumentException(
                    "User account is already enabled");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);

        Users updatedUser = usersRepository.save(user);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        AuditLog auditLog = new AuditLog();

        auditLog.setAction("ACCOUNT_ENABLED_BY_ADMIN");
        auditLog.setIdentifier(user.getEmail());
        auditLog.setTimestamp(LocalDateTime.now());

        auditLog.setDetails(
                String.format(
                        "Admin '%s' enabled account '%s' (User ID: %d)",
                        Objects.requireNonNull(authentication).getName(),
                        user.getEmail(),
                        user.getId()
                )
        );

        auditLogRepository.save(auditLog);

        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UsersResponse disableUser(Long userId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id: " + userId));

        // Prevent disabling default admin
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Default admin account cannot be disabled");
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String adminName = Objects.requireNonNull(authentication).getName();

        // Already disabled
        if (!user.isEnabled()) {
            throw new IllegalArgumentException(
                    "User account is already disabled");
        }

        // Optional safety: prevent self-disable
        if (user.getUsername().equals(adminName)) {
            throw new IllegalArgumentException(
                    "You cannot disable your own account");
        }

        //  Core change
        user.setEnabled(false);
        user.setStatus(UserStatus.DISABLED);

        // Optional hardening (recommended in real systems)
        user.setAccountNonLocked(false);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);

        Users updatedUser = usersRepository.save(user);

        // Audit log
        AuditLog auditLog = new AuditLog();
        auditLog.setIdentifier(user.getEmail());
        auditLog.setAction("ACCOUNT_DISABLED_BY_ADMIN");
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(
                String.format(
                        "Admin '%s' disabled account '%s' (User ID: %d)",
                        adminName,
                        user.getEmail(),
                        user.getId()
                )
        );

        auditLogRepository.save(auditLog);

        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UsersResponse deleteUser(Long userId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id: " + userId));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String adminName = Objects.requireNonNull(authentication).getName();

        //  Prevent deleting default admin
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Default admin account cannot be deleted");
        }

        //  Already deleted
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("User is already deleted");
        }

        //  Prevent self-delete
        if (user.getUsername().equals(adminName)) {
            throw new IllegalArgumentException(
                    "You cannot delete your own account");
        }

        //  SOFT DELETE
        user.setStatus(UserStatus.DELETED);
        user.setEnabled(false);
        user.setAccountNonLocked(false);

        Users updatedUser = usersRepository.save(user);

        //  Audit log
        AuditLog auditLog = new AuditLog();
        auditLog.setIdentifier(user.getEmail());
        auditLog.setAction("ACCOUNT_DELETED_BY_ADMIN");
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(
                String.format(
                        "Admin '%s' deleted account '%s' (User ID: %d)",
                        adminName,
                        user.getEmail(),
                        user.getId()
                )
        );

        auditLogRepository.save(auditLog);

        return mapToUserResponse(updatedUser);
    }

    @Override
    public AdminStatsResponse getAdminStats() {

        log.info("[ADMIN_STATS] Admin statistics retrieval started.");

        List<Users> users = usersRepository.findAll();

        long totalUsers = users.size();

        long activeUsers =
                usersRepository.countByStatus(UserStatus.ACTIVE);

        long lockedUsers =
                usersRepository.countByStatus(UserStatus.LOCKED);

        long deletedUsers =
                usersRepository.countByStatus(UserStatus.DELETED);

        log.debug(
                "[ADMIN_STATS] Statistics calculated. TotalUsers={}, ActiveUsers={}, LockedUsers={}, DeletedUsers={}",
                totalUsers,
                activeUsers,
                lockedUsers,
                deletedUsers);

        AdminStatsResponse response =
                AdminStatsResponse.builder()
                        .totalUsers(totalUsers)
                        .activeUsers(activeUsers)
                        .lockedUsers(lockedUsers)
                        .deletedUsers(deletedUsers)
                        .build();

        log.info(
                "[ADMIN_STATS] Admin statistics retrieval completed successfully.");

        return response;
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
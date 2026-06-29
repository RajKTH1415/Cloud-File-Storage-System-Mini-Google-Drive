package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.response.*;
import com.cloudFileStorageSystem.enums.Role;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminService {

    UnlockUserResponse unlockUser(Long userId);

    List<UsersResponse> getAllUsers();

    UsersResponse getUserById(Long userId);

    UsersResponse updateUserRole(Long userId, Role role);

    LockUserResponse lockUser(Long userId);
    UsersResponse enableUser(Long userId);

    UsersResponse disableUser(Long userId);

    UsersResponse deleteUser(Long userId);

    AdminStatsResponse getAdminStats();

    PageResponse<AuditLogResponse> getAuditLogs(
            String identifier,
            String action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size,
            String sortBy,
            String direction);
}
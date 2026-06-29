package com.cloudFileStorageSystem.controller;
import com.cloudFileStorageSystem.dtos.request.UpdateUserRoleRequest;
import com.cloudFileStorageSystem.dtos.response.*;
import com.cloudFileStorageSystem.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats(HttpServletRequest request) {
        log.info("[ADMIN_STATS] Admin statistics request received. IP={}, URI={}", request.getRemoteAddr(), request.getRequestURI());
        AdminStatsResponse response = adminService.getAdminStats();
        log.info("[ADMIN_STATS] Admin statistics fetched successfully.");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Admin stats fetched successfully", request.getRequestURI(), response));
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<ApiResponse<UnlockUserResponse>> unlockUser(@PathVariable Long id, HttpServletRequest request) {
        log.info("[UNLOCK_USER] Unlock user request received. UserId={}, IP={}, URI={}", id, request.getRemoteAddr(), request.getRequestURI());
        UnlockUserResponse response = adminService.unlockUser(id);
        log.info("[UNLOCK_USER] User unlocked successfully. UserId={}", id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "User unlocked successfully", request.getRequestURI(), response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UsersResponse>>> getAllUsers(HttpServletRequest request) {
        log.info("[GET_ALL_USERS] Fetch all users request received. IP={}, URI={}", request.getRemoteAddr(), request.getRequestURI());
        List<UsersResponse> users = adminService.getAllUsers();
        log.info("[GET_ALL_USERS] Users fetched successfully. TotalUsers={}", users.size());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Users fetched successfully", request.getRequestURI(), users));
    }
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UsersResponse>> getUserById(@PathVariable Long id, HttpServletRequest request) {
        log.info("[GET_USER_BY_ID] User retrieval request received. UserId={}, IP={}, URI={}", id, request.getRemoteAddr(), request.getRequestURI());
        UsersResponse user = adminService.getUserById(id);
        log.info("[GET_USER_BY_ID] User retrieved successfully. UserId={}", id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "User fetched successfully", request.getRequestURI(), user));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UsersResponse>> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request, HttpServletRequest servletRequest) {
        log.info("[UPDATE_USER_ROLE] Role update request received. UserId={}, NewRole={}, IP={}, URI={}", id, request.getRole(), servletRequest.getRemoteAddr(), servletRequest.getRequestURI());
        UsersResponse response = adminService.updateUserRole(id, request.getRole());
        log.info("[UPDATE_USER_ROLE] Role updated successfully. UserId={}, NewRole={}", id, request.getRole());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "User role updated successfully", servletRequest.getRequestURI(), response));
    }

    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<ApiResponse<LockUserResponse>> lockUser(@PathVariable Long id, HttpServletRequest request) {
        log.info("[LOCK_USER] Lock user request received. UserId={}, IP={}, URI={}", id, request.getRemoteAddr(), request.getRequestURI());
        LockUserResponse response = adminService.lockUser(id);
        log.info("[LOCK_USER] User locked successfully. UserId={}", id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "User locked successfully", request.getRequestURI(), response));
    }

    @PatchMapping("/users/{id}/enable")
    public ResponseEntity<ApiResponse<UsersResponse>> enableUser(@PathVariable Long id, HttpServletRequest request) {
        log.info("[ENABLE_USER] Enable user request received. UserId={}, IP={}, URI={}", id, request.getRemoteAddr(), request.getRequestURI());
        UsersResponse response = adminService.enableUser(id);
        log.info("[ENABLE_USER] User enabled successfully. UserId={}", id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "User enabled successfully", request.getRequestURI(), response));
    }

    @PatchMapping("/users/{id}/disable")
    public ResponseEntity<ApiResponse<UsersResponse>> disableUser(@PathVariable Long id, HttpServletRequest request) {
        log.info("[DISABLE_USER] Disable user request received. UserId={}, IP={}, URI={}", id, request.getRemoteAddr(), request.getRequestURI());
        UsersResponse response = adminService.disableUser(id);
        log.info("[DISABLE_USER] User disabled successfully. UserId={}", id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "User disabled successfully", request.getRequestURI(), response));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UsersResponse>> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        log.info("[DELETE_USER] Delete user request received. UserId={}, IP={}, URI={}", id, request.getRemoteAddr(), request.getRequestURI());
        UsersResponse response = adminService.deleteUser(id);
        log.info("[DELETE_USER] User deleted successfully. UserId={}", id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "User deleted successfully", request.getRequestURI(), response));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(

            @RequestParam(required = false) String identifier,

            @RequestParam(required = false) String action,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "timestamp") String sortBy,

            @RequestParam(defaultValue = "DESC") String direction,

            HttpServletRequest request) {

        log.info(
                "[GET_AUDIT_LOGS] Request received. identifier={}, action={}, page={}, size={}, sortBy={}, direction={}",
                identifier,
                action,
                page,
                size,
                sortBy,
                direction);

        PageResponse<AuditLogResponse> response =
                adminService.getAuditLogs(
                        identifier,
                        action,
                        startDate,
                        endDate,
                        page,
                        size,
                        sortBy,
                        direction);

        log.info(
                "[GET_AUDIT_LOGS] Audit logs fetched successfully. TotalRecords={}",
                response.getTotalElements());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Audit logs fetched successfully",
                        request.getRequestURI(),
                        response));
    }
}
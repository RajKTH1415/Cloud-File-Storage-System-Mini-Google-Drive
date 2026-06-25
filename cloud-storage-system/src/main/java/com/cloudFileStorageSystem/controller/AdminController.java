package com.cloudFileStorageSystem.controller;


import com.cloudFileStorageSystem.dtos.request.UpdateUserRoleRequest;
import com.cloudFileStorageSystem.dtos.response.ApiResponse;
import com.cloudFileStorageSystem.dtos.response.LockUserResponse;
import com.cloudFileStorageSystem.dtos.response.UnlockUserResponse;
import com.cloudFileStorageSystem.dtos.response.UsersResponse;
import com.cloudFileStorageSystem.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<ApiResponse<UnlockUserResponse>> unlockUser(@PathVariable Long id, HttpServletRequest request) {
        log.info("Received unlock user request for userId: {}", id);
        UnlockUserResponse unlockUserResponse = adminService.unlockUser(id);
        log.info("User unlocked successfully for userId: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "User unlocked successfully", request.getRequestURI(), unlockUserResponse));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UsersResponse>>> getAllUsers(HttpServletRequest request) {
        log.info("Fetching all users");
        List<UsersResponse> users = adminService.getAllUsers();
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Users fetched successfully", request.getRequestURI(), users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UsersResponse>> getUserById(@PathVariable Long id, HttpServletRequest request) {
        log.info("Fetching user with id={}", id);
        UsersResponse user = adminService.getUserById(id);
        return  ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "User fetched successfully", request.getRequestURI(), user));
    }
    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UsersResponse>> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request, HttpServletRequest servletRequest) {
        log.info("Role update request received. userId={}, role={}", id, request.getRole());
        UsersResponse response = adminService.updateUserRole(id, request.getRole());
        log.info("Role updated successfully. userId={}, role={}", id, request.getRole());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "User role updated successfully", servletRequest.getRequestURI(), response));

    }

    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<ApiResponse<LockUserResponse>> lockUser(@PathVariable Long id, HttpServletRequest request) {
        log.info("Lock user request received for userId={}", id);
        LockUserResponse response = adminService.lockUser(id);
        log.info("User locked successfully. userId={}", id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "User locked successfully", request.getRequestURI(), response));
    }
}
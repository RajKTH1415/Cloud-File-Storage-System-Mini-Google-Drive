package com.cloudFileStorageSystem.controller;


import com.cloudFileStorageSystem.dtos.response.ApiResponse;
import com.cloudFileStorageSystem.dtos.response.UnlockUserResponse;
import com.cloudFileStorageSystem.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
package com.cloudFileStorageSystem.controller;

import com.cloudFileStorageSystem.dtos.request.LoginRequest;
import com.cloudFileStorageSystem.dtos.request.RefreshTokenRequest;
import com.cloudFileStorageSystem.dtos.response.ApiResponse;
import com.cloudFileStorageSystem.dtos.response.LoginResponse;
import com.cloudFileStorageSystem.dtos.response.LogoutResponse;
import com.cloudFileStorageSystem.dtos.response.TokenResponse;
import com.cloudFileStorageSystem.module.TokenData;
import com.cloudFileStorageSystem.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        log.info("POST {} invoked for identifier={}", httpServletRequest.getRequestURI(), request.getIdentifier());
        LoginResponse response = authService.login(request, httpServletRequest);
        log.info("Login API completed successfully for username={}", response.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Login successful", httpServletRequest.getRequestURI(), response));
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest, @RequestHeader("Authorization") String authorizationHeader) {
        String accessToken = authorizationHeader.replace("Bearer ", "");
        log.info("Logout request received. URI={}, IP={}", servletRequest.getRequestURI(), servletRequest.getRemoteAddr());
        LogoutResponse logoutResponse = authService.logout(accessToken, request.getRefreshToken());
        log.info("Logout completed successfully");
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(),"Logout successful", servletRequest.getRequestURI(), logoutResponse));
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        TokenResponse response = authService.refreshToken(request, httpServletRequest);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Token refreshed successfully", httpServletRequest.getRequestURI(), response));
    }
}

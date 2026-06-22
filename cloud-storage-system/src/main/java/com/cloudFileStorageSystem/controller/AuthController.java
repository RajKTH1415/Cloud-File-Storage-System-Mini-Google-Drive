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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        LoginResponse response = authService.login(request, httpServletRequest);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Login successful", httpServletRequest.getRequestURI(), response));
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest, @RequestHeader("Authorization") String authorizationHeader) {
        String accessToken = authorizationHeader.replace("Bearer ", "");
        LogoutResponse logoutResponse = authService.logout(accessToken, request.getRefreshToken());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(),"Logout successful", servletRequest.getRequestURI(), logoutResponse));
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        TokenResponse response = authService.refreshToken(request, httpServletRequest);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Token refreshed successfully", httpServletRequest.getRequestURI(), response));
    }
}

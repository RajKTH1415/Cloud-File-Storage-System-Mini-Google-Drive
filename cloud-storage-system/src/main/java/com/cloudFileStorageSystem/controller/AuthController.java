package com.cloudFileStorageSystem.controller;

import com.cloudFileStorageSystem.dtos.request.*;
import com.cloudFileStorageSystem.dtos.response.*;
import com.cloudFileStorageSystem.service.AuthService;
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
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        log.info("[LOGIN] Login request received. Identifier={}, IP={}, URI={}", request.getIdentifier(), httpServletRequest.getRemoteAddr(), httpServletRequest.getRequestURI());
        LoginResponse response = authService.login(request, httpServletRequest);
        log.info("[LOGIN] Login completed successfully., Username={}",  response.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Login successful", httpServletRequest.getRequestURI(), response));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest, @RequestHeader("Authorization") String authorizationHeader) {
        String accessToken = authorizationHeader.replace("Bearer ", "");
        log.info("[LOGOUT] Logout request received. IP={}, URI={}", servletRequest.getRemoteAddr(), servletRequest.getRequestURI());
        LogoutResponse logoutResponse = authService.logout(accessToken, request.getRefreshToken());
        log.info("[LOGOUT] Logout completed successfully.");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Logout successful", servletRequest.getRequestURI(), logoutResponse));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        log.info("[REFRESH_TOKEN] Refresh token request received. IP={}, URI={}", httpServletRequest.getRemoteAddr(), httpServletRequest.getRequestURI());
        TokenResponse response = authService.refreshToken(request, httpServletRequest);
        log.info("[REFRESH_TOKEN] Token refresh completed successfully.");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Token refreshed successfully", httpServletRequest.getRequestURI(), response));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token, HttpServletRequest request) {
        log.info("[VERIFY_EMAIL] Verification request received. IP={}, URI={}", request.getRemoteAddr(), request.getRequestURI());
        authService.verifyEmail(token);
        log.info("[VERIFY_EMAIL] Email verification completed successfully.");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Email verified successfully", request.getRequestURI(), "Account activated"));
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<ApiResponse<ResendVerificationEmailResponse>> resendVerificationEmail(@RequestBody @Valid ResendVerificationRequest request, HttpServletRequest httpServletRequest) {
        log.info("[RESEND_VERIFICATION_EMAIL] Resend verification email request received. Email={}, IP={}, URI={}", request.getEmail(), httpServletRequest.getRemoteAddr(), httpServletRequest.getRequestURI());
        ResendVerificationEmailResponse response = authService.resendVerificationEmail(request.getEmail());
        log.info("[RESEND_VERIFICATION_EMAIL] Verification email resent successfully. Email={}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Verification email resent successfully", httpServletRequest.getRequestURI(), response));
    }

    @PostMapping("/forgot-password/email")
    public ResponseEntity<ApiResponse<OtpResponse>> forgotPasswordEmail(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        log.info("[FORGOT_PASSWORD] Forgot password request received. Email={}, IP={}, URI={}", request.getEmail(), servletRequest.getRemoteAddr(), servletRequest.getRequestURI());
        OtpResponse response = authService.forgotPassword(request);
        log.info("[FORGOT_PASSWORD] Forgot password request completed successfully. Email={}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "OTP sent successfully", servletRequest.getRequestURI(), response));
    }

    @PostMapping("/resend-password-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> resendPasswordOtp(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpServletRequest) {
        log.info("[RESEND_PASSWORD_OTP] Resend password OTP request received. Email={}, IP={}, URI={}", request.getEmail(), httpServletRequest.getRemoteAddr(), httpServletRequest.getRequestURI());
        OtpResponse response = authService.resendPasswordOtp(request.getEmail());
        log.info("[RESEND_PASSWORD_OTP] Password OTP resent successfully. Email={}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "OTP resent successfully", httpServletRequest.getRequestURI(), response));
    }

    @PostMapping("/verify-password-otp")
    public ResponseEntity<ApiResponse<EmailOtpVerifyResponse>> EmailVerifyOtp(@RequestBody VerifyEmailOtpRequest verifyEmailOtpRequest, HttpServletRequest servletRequest) {
        log.info("[VERIFY_PASSWORD_OTP] Password OTP verification request received. Email={}, IP={}, URI={}", verifyEmailOtpRequest.getEmail(), servletRequest.getRemoteAddr(), servletRequest.getRequestURI());
        EmailOtpVerifyResponse response = authService.verifyEmailPasswordOtp(verifyEmailOtpRequest);
        log.info("[VERIFY_PASSWORD_OTP] Password OTP verified successfully. Email={}", verifyEmailOtpRequest.getEmail());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "OTP verified successfully", servletRequest.getRequestURI(), response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@RequestBody ResetPasswordRequest request, HttpServletRequest servletRequest) {
        log.info("[RESET_PASSWORD] Password reset request received. Email={}, IP={}, URI={}", request.getEmail(), servletRequest.getRemoteAddr(), servletRequest.getRequestURI());
        ResetPasswordResponse response = authService.resetPassword(request, servletRequest);
        log.info("[RESET_PASSWORD] Password reset completed successfully. Email={}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Password changed successfully. Please login again.", servletRequest.getRequestURI(), response));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        log.info("[CHANGE_PASSWORD] Change password request received. IP={}, URI={}", httpRequest.getRemoteAddr(), httpRequest.getRequestURI());
        ChangePasswordResponse response = authService.changePassword(request, httpRequest);
        log.info("[CHANGE_PASSWORD] Password changed successfully.");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Password changed successfully", httpRequest.getRequestURI(), response));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/login-history")
    public ResponseEntity<?> getLoginHistory(@RequestParam Long userId, HttpServletRequest httpServletRequest) {
        log.info("Received login history request for userId: {}", userId);
        List<LoginHistoryResponse> historyResponses = authService.getLoginHistory(userId);
        log.info("Successfully fetched {} login history records for userId: {}", historyResponses.size(), userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Login history fetched successfully", httpServletRequest.getRequestURI(), historyResponses));
    }

}

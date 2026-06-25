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
        log.info("POST {} invoked for identifier={}", httpServletRequest.getRequestURI(), request.getIdentifier());
        LoginResponse response = authService.login(request, httpServletRequest);
        log.info("Login API completed successfully for username={}", response.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Login successful", httpServletRequest.getRequestURI(), response));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
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
        log.info("Refresh token request received. URI={}, IP={}", httpServletRequest.getRequestURI(), httpServletRequest.getRemoteAddr());
        TokenResponse response = authService.refreshToken(request, httpServletRequest);
        log.info("Token refresh completed successfully");
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Token refreshed successfully", httpServletRequest.getRequestURI(), response));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token, HttpServletRequest request) {
        log.info("Received email verification request");
        authService.verifyEmail(token);
        log.info("Email verified successfully");
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Email verified successfully", request.getRequestURI(), "Account activated"));
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<ApiResponse<ResendVerificationEmailResponse>> resendVerificationEmail(@RequestBody @Valid ResendVerificationRequest request, HttpServletRequest httpServletRequest) {
        log.info("Received resend verification email request for email: {}", request.getEmail());
        ResendVerificationEmailResponse resendVerificationEmailResponse = authService.resendVerificationEmail(request.getEmail());
        log.info("Verification email resent successfully for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Verification email resent successfully", httpServletRequest.getRequestURI(), resendVerificationEmailResponse));
    }

    @PostMapping("/forgot-password/email")
    public ResponseEntity<ApiResponse<OtpResponse>> forgotPasswordEmail(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        log.info("Forgot password API called for email: {}", request.getEmail());
        OtpResponse response = authService.forgotPassword(request);
        log.info("Forgot password API completed successfully for email: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "OTP sent successfully", servletRequest.getRequestURI(), response));
    }

    @PostMapping("/resend-password-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> resendPasswordOtp(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpServletRequest) {
        log.info("Received resend password OTP request for email: {}", request.getEmail());
        OtpResponse response = authService.resendPasswordOtp(request.getEmail());
        log.info("Password OTP resent successfully for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "OTP resent successfully", httpServletRequest.getRequestURI(), response));
    }

    @PostMapping("/verify-password-otp")
    public ResponseEntity<ApiResponse<EmailOtpVerifyResponse>> EmailVerifyOtp(@RequestBody VerifyEmailOtpRequest verifyEmailOtpRequest, HttpServletRequest servletRequest) {
        log.info("Received password OTP verification request for email: {}", verifyEmailOtpRequest.getEmail());
        EmailOtpVerifyResponse otpVerifyResponse = authService.verifyEmailPasswordOtp(verifyEmailOtpRequest);
        log.info("Password OTP verified successfully for email: {}", verifyEmailOtpRequest.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "OTP verified successfully", servletRequest.getRequestURI(), otpVerifyResponse));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@RequestBody ResetPasswordRequest request, HttpServletRequest servletRequest) {
        log.info("Received password reset request for email: {}", request.getEmail());
        ResetPasswordResponse resetPasswordResponse = authService.resetPassword(request, servletRequest);
        log.info("Password reset completed successfully for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Password changed successfully. Please login again.", servletRequest.getRequestURI(), resetPasswordResponse));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        log.info("Received change password request");
        ChangePasswordResponse response = authService.changePassword(request, httpRequest);
        log.info("Password changed successfully");
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Password changed successfully", httpRequest.getRequestURI(), response));
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

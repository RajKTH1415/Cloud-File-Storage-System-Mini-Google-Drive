package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.request.*;
import com.cloudFileStorageSystem.dtos.response.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;


public interface AuthService {

    LoginResponse login(LoginRequest loginRequest, HttpServletRequest httpServletRequest);

    TokenResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpServletRequest);

    OtpResponse forgotPassword(ForgotPasswordRequest request);


    LogoutResponse logout(String accessToken, String refreshToken);

    void verifyEmail(String token);

    EmailOtpVerifyResponse verifyEmailPasswordOtp(VerifyEmailOtpRequest verifyEmailOtpRequest);

    ResetPasswordResponse resetPassword(
            ResetPasswordRequest request,
            HttpServletRequest httpServletRequest
    );
    ChangePasswordResponse changePassword(
            ChangePasswordRequest request,
            HttpServletRequest httpRequest
    );

    List<LoginHistoryResponse> getLoginHistory(Long userId);

    ResendVerificationEmailResponse resendVerificationEmail(String email);

    OtpResponse resendPasswordOtp(String email);

}


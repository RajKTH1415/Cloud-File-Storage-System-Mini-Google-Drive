package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.request.ForgotPasswordRequest;
import com.cloudFileStorageSystem.dtos.request.LoginRequest;
import com.cloudFileStorageSystem.dtos.request.RefreshTokenRequest;
import com.cloudFileStorageSystem.dtos.request.VerifyEmailOtpRequest;
import com.cloudFileStorageSystem.dtos.response.*;
import jakarta.servlet.http.HttpServletRequest;


public interface AuthService {

    LoginResponse login(LoginRequest loginRequest, HttpServletRequest httpServletRequest);

    TokenResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpServletRequest);

    OtpResponse forgotPassword(ForgotPasswordRequest request);


    LogoutResponse logout(String accessToken, String refreshToken);

    void verifyEmail(String token);

    EmailOtpVerifyResponse verifyEmailPasswordOtp(VerifyEmailOtpRequest verifyEmailOtpRequest);
}

package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.request.ForgotPasswordRequest;
import com.cloudFileStorageSystem.dtos.request.LoginRequest;
import com.cloudFileStorageSystem.dtos.request.RefreshTokenRequest;
import com.cloudFileStorageSystem.dtos.response.LoginResponse;
import com.cloudFileStorageSystem.dtos.response.LogoutResponse;
import com.cloudFileStorageSystem.dtos.response.OtpResponse;
import com.cloudFileStorageSystem.dtos.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;


public interface AuthService {

    LoginResponse login(LoginRequest loginRequest, HttpServletRequest httpServletRequest);

    TokenResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpServletRequest);

    OtpResponse forgotPassword(ForgotPasswordRequest request);


    LogoutResponse logout(String accessToken, String refreshToken);
}

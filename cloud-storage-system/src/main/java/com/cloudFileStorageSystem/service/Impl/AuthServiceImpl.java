package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.dtos.request.LoginRequest;
import com.cloudFileStorageSystem.dtos.request.RefreshTokenRequest;
import com.cloudFileStorageSystem.dtos.response.LoginResponse;
import com.cloudFileStorageSystem.dtos.response.LogoutResponse;
import com.cloudFileStorageSystem.dtos.response.TokenResponse;
import com.cloudFileStorageSystem.module.RefreshToken;
import com.cloudFileStorageSystem.module.TokenData;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.RefreshTokenRepository;
import com.cloudFileStorageSystem.repository.UsersRepository;
import com.cloudFileStorageSystem.service.AuthService;
import com.cloudFileStorageSystem.service.TokenBlacklistService;
import com.cloudFileStorageSystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {


    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsersRepository usersRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(TokenBlacklistService tokenBlacklistService, RefreshTokenRepository refreshTokenRepository, UsersRepository usersRepository, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.usersRepository = usersRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }


    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
        log.info("Login attempt received. Identifier={}, IP={}", request.getIdentifier(), httpServletRequest.getRemoteAddr());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()));
        log.info("Authentication successful for identifier={}",
                request.getIdentifier());

        Users user = usersRepository
                .findByUsernameOrEmailOrPhoneNumber(
                        request.getIdentifier(),
                        request.getIdentifier(),
                        request.getIdentifier()
                )
                .orElseThrow(() -> {
                    log.warn("User not found. Identifier={}",
                            request.getIdentifier());
                    return new RuntimeException("User not found");});

        log.debug("User found. UserId={}, Username={}, Role={}", user.getId(), user.getUsername(), user.getRole());

        String accessToken =
                jwtUtil.generateAccessToken(user);

        String refreshToken =
                jwtUtil.generateRefreshToken(user);

        log.info("JWT tokens generated successfully for userId={}",
                user.getId());
        TokenData tokens = TokenData.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        log.info("Login completed successfully. UserId={}, Username={}",
                user.getId(),
                user.getUsername());

        return LoginResponse.builder()
                .username(user.getUsername())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .tokens(tokens)
                .build();
    }

    @Override
    public TokenResponse refreshToken(
            RefreshTokenRequest request,
            HttpServletRequest httpServletRequest) {

        log.info("Refresh token process started");

        String refreshToken = request.getRefreshToken();

        RefreshToken storedToken =
                refreshTokenRepository
                        .findByToken(refreshToken)
                        .orElseThrow(() -> {
                            log.warn("Refresh token not found");
                            return new RuntimeException(
                                    "Refresh token not found");
                        });

        log.debug("Refresh token located. TokenId={}, UserId={}",
                storedToken.getId(),
                storedToken.getUserId());

        if (storedToken.getRevoked()) {
            log.warn("Attempt to use revoked refresh token. TokenId={}, UserId={}",
                    storedToken.getId(),
                    storedToken.getUserId());

            throw new RuntimeException(
                    "Refresh token revoked");
        }

        if (!jwtUtil.isTokenValid(refreshToken)) {

            log.warn("Expired refresh token detected. TokenId={}, UserId={}",
                    storedToken.getId(),
                    storedToken.getUserId());

            throw new RuntimeException(
                    "Refresh token expired");
        }

        Long userId =
                jwtUtil.extractUserId(refreshToken);

        log.debug("User ID extracted from refresh token. UserId={}",
                userId);

        Users user =
                usersRepository.findById(userId)
                        .orElseThrow(() -> {
                            log.warn("User not found for refresh token. UserId={}",
                                    userId);
                            return new RuntimeException("User not found");
                        });

        log.debug("User located successfully. UserId={}, Username={}",
                user.getId(),
                user.getUsername());

        // Revoke old refresh token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        log.info("Old refresh token revoked. TokenId={}, UserId={}",
                storedToken.getId(),
                user.getId());

        // Generate new tokens
        String newAccessToken =
                jwtUtil.generateAccessToken(user);

        String newRefreshToken =
                jwtUtil.generateRefreshToken(user);

        log.info("New JWT tokens generated. UserId={}",
                user.getId());

        // Save new refresh token
        RefreshToken newToken =
                RefreshToken.builder()
                        .userId(user.getId())
                        .token(newRefreshToken)
                        .expiryDate(
                                jwtUtil.extractExpiration(
                                        newRefreshToken))
                        .revoked(false)
                        .build();

        refreshTokenRepository.save(newToken);

        log.info("New refresh token saved. TokenId={}, UserId={}",
                newToken.getId(),
                user.getId());

        log.info("Token refresh completed successfully. UserId={}",
                user.getId());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    @Transactional
    public LogoutResponse logout(String accessToken, String refreshToken) {

        log.info("Logout process started");

        // Blacklist access token
        tokenBlacklistService.blacklistToken(accessToken);

        log.debug("Access token blacklisted successfully");

        // Revoke refresh token
        RefreshToken token =
                refreshTokenRepository
                        .findByToken(refreshToken)
                        .orElseThrow(() -> {
                            log.warn("Logout failed. Invalid refresh token");
                            return new RuntimeException("Invalid refresh token");
                        });

        log.debug("Refresh token found. TokenId={}, UserId={}",
                token.getId(),
                token.getUserId());

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        log.info("Refresh token revoked successfully. TokenId={}, UserId={}",
                token.getId(),
                token.getUserId());

        log.info("Logout completed successfully. UserId={}",
                token.getUserId());

        return LogoutResponse.builder()
                .loggedOut(true)
                .tokenRevoked(true)
                .build();
    }
}

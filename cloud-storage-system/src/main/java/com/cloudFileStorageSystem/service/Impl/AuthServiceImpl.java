package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.dtos.request.ForgotPasswordRequest;
import com.cloudFileStorageSystem.dtos.request.LoginRequest;
import com.cloudFileStorageSystem.dtos.request.RefreshTokenRequest;
import com.cloudFileStorageSystem.dtos.response.LoginResponse;
import com.cloudFileStorageSystem.dtos.response.LogoutResponse;
import com.cloudFileStorageSystem.dtos.response.OtpResponse;
import com.cloudFileStorageSystem.dtos.response.TokenResponse;
import com.cloudFileStorageSystem.enums.OtpPurpose;
import com.cloudFileStorageSystem.module.Otp;
import com.cloudFileStorageSystem.module.RefreshToken;
import com.cloudFileStorageSystem.module.TokenData;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.OtpRepository;
import com.cloudFileStorageSystem.repository.RefreshTokenRepository;
import com.cloudFileStorageSystem.repository.UsersRepository;
import com.cloudFileStorageSystem.service.AuthService;
import com.cloudFileStorageSystem.service.EmailService;
import com.cloudFileStorageSystem.service.TokenBlacklistService;
import com.cloudFileStorageSystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private  final OtpRepository otpRepository;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsersRepository usersRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    public AuthServiceImpl(OtpRepository otpRepository, EmailService emailService, TokenBlacklistService tokenBlacklistService, RefreshTokenRepository refreshTokenRepository, UsersRepository usersRepository, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
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

        // Revoke previous active refresh tokens
        refreshTokenRepository
                .findByUserIdAndRevokedFalse(user.getId())
                .forEach(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });

        // Save new refresh token
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiryDate(
                        new Date(System.currentTimeMillis() + refreshTokenValidity)
                )
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        refreshTokenRepository.save(refreshTokenEntity);

        log.info("Refresh token saved successfully. UserId={}",
                user.getId());


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
    public OtpResponse forgotPassword(ForgotPasswordRequest request) {

        log.info("Forgot password request received for email: {}", request.getEmail());

        Users user = usersRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Forgot password requested for non-existing email: {}", request.getEmail());
                    return new RuntimeException("User not found");
                });

        otpRepository.deleteByUserAndPurpose(
                user,
                OtpPurpose.FORGOT_PASSWORD
        );

        log.info("Existing forgot-password OTPs removed for user: {}", user.getEmail());

        String otpCode = String.format(
                "%06d",
                ThreadLocalRandom.current().nextInt(100000, 1000000)
        );

        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

        Otp otp = Otp.builder()
                .user(user)
                .otpCode(otpCode)
                .purpose(OtpPurpose.FORGOT_PASSWORD)
                .verified(false)
                .attemptCount(0)
                .expiryTime(expiryTime)
                .build();

        otpRepository.save(otp);

        log.info(
                "Forgot-password OTP generated for user: {} with expiry at {}",
                user.getEmail(),
                expiryTime
        );

        emailService.sendOtpEmail(
                user.getEmail(),
                otpCode
        );

        log.info("Forgot-password OTP email sent successfully to: {}", user.getEmail());

        return OtpResponse.builder()
                .email(user.getEmail())
                .expiryMinutes(otpExpiryMinutes)
                .emailSent(true)
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

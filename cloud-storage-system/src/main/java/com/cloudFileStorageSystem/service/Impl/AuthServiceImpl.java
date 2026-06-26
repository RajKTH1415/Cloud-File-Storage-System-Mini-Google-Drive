package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.config.SecurityProperties;
import com.cloudFileStorageSystem.dtos.request.*;
import com.cloudFileStorageSystem.dtos.response.*;
import com.cloudFileStorageSystem.enums.AttemptStatus;
import com.cloudFileStorageSystem.enums.OtpPurpose;
import com.cloudFileStorageSystem.module.*;
import com.cloudFileStorageSystem.repository.*;
import com.cloudFileStorageSystem.security.CustomUserDetailsService;
import com.cloudFileStorageSystem.service.AuditService;
import com.cloudFileStorageSystem.service.AuthService;
import com.cloudFileStorageSystem.service.EmailService;
import com.cloudFileStorageSystem.service.TokenBlacklistService;
import com.cloudFileStorageSystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


@Service
public class AuthServiceImpl implements AuthService {


    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final SecurityProperties securityProperties;
    private final AuditLogRepository auditLogRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsersRepository usersRepository;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    @Value("${security.password-history-limit}")
    private int passwordHistoryLimit;


    public AuthServiceImpl(PasswordHistoryRepository passwordHistoryRepository, PasswordResetOtpRepository passwordResetOtpRepository, SecurityProperties securityProperties, AuditLogRepository auditLogRepository, LoginAttemptRepository loginAttemptRepository, PasswordEncoder passwordEncoder, AuditService auditService, EmailVerificationTokenRepository emailVerificationTokenRepository, OtpRepository otpRepository, EmailService emailService, TokenBlacklistService tokenBlacklistService, RefreshTokenRepository refreshTokenRepository, UsersRepository usersRepository, JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService) {
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.securityProperties = securityProperties;
        this.auditLogRepository = auditLogRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.usersRepository = usersRepository;
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
    }


    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {

        String identifier = request.getIdentifier();

        log.info("[LOGIN] Login process started. Identifier={}", identifier);

        Users user = usersRepository
                .findByUsernameOrEmailOrPhoneNumber(
                        identifier,
                        identifier,
                        identifier
                )
                .orElseThrow(() -> {

                    log.warn("[LOGIN] Login failed. User not found. Identifier={}", identifier);

                    saveUnknownUserAttempt(
                            identifier,
                            httpServletRequest,
                            "USER_NOT_FOUND"
                    );
                    saveAuditLog(
                            identifier,
                            "LOGIN_FAILED",
                            httpServletRequest,
                            "User not found"
                    );

                    return new RuntimeException(
                            "Invalid username/email/phone or password"
                    );
                });

        log.info(
                "[LOGIN] User located. UserId={}, Username={}, FailedAttempts={}, AccountLocked={}",
                user.getId(),
                user.getUsername(),
                user.getFailedAttempts(),
                !user.isAccountNonLocked()
        );

        validateAccountStatus(user);

        boolean passwordMatched =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        log.info("[LOGIN] Password verified successfully. Username={}",
                user.getUsername());

        if (!passwordMatched) {

            log.warn("[LOGIN] Login failed. Invalid password. Username={}",
                    user.getUsername());

            processFailedLogin(
                    user,
                    request,
                    httpServletRequest
            );

            throw new RuntimeException(
                    "Invalid username/email/phone or password"
            );
        }

        log.info(
                "Password verified successfully for username={}",
                user.getUsername()
        );

        resetFailedAttempts(user);

        user.setLastLoginAt(LocalDateTime.now());

        usersRepository.save(user);

        log.info("[LOGIN] User account updated after successful login. UserId={}",
                user.getId());

        saveSuccessfulAttempt(
                user,
                request.getIdentifier(),
                httpServletRequest
        );
        saveAuditLog(
                user.getEmail(),
                "LOGIN_SUCCESS",
                httpServletRequest,
                "User logged in successfully"
        );
        String accessToken =
                jwtUtil.generateAccessToken(user);

        String refreshToken =
                jwtUtil.generateRefreshToken(user);

        log.debug("[LOGIN] JWT access and refresh tokens generated. UserId={}",
                user.getId());

        RefreshToken refreshTokenEntity =
                RefreshToken.builder()
                        .userId(user.getId())
                        .token(refreshToken)
                        .expiryDate(
                                new Date(
                                        System.currentTimeMillis()
                                                + refreshTokenValidity
                                )
                        )
                        .revoked(false)
                        .build();

        refreshTokenRepository.save(refreshTokenEntity);

        refreshTokenRepository.save(
                refreshTokenEntity
        );

        TokenData tokenData = TokenData.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        log.info("[LOGIN] Login completed successfully. UserId={}, Username={}",
                user.getId(),
                user.getUsername());

        return LoginResponse.builder()
                .username(user.getUsername())
                .fullName(
                        user.getFirstName()
                                + " "
                                + user.getLastName()
                )
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .tokens(tokenData)
                .build();
    }

    private void validateAccountStatus(Users user) {

        log.debug("[LOGIN] Validating account status. Username={}",
                user.getUsername());

        if (!user.isEnabled()) {
            log.warn("[LOGIN] Login failed. Account disabled. Username={}",
                    user.getUsername());
            throw new RuntimeException("Account is disabled");
        }

        if (!user.isEmailVerified()) {
            log.warn("[LOGIN] Login failed. Email not verified. Username={}",
                    user.getUsername());
            throw new RuntimeException(
                    "Please verify your email first"
            );
        }

        if (!user.isAccountNonLocked()) {

            if (user.getLockedUntil() != null &&
                    user.getLockedUntil().isAfter(LocalDateTime.now())) {

                log.warn("[LOGIN] Account locked until {}. Username={}",
                        user.getLockedUntil(),
                        user.getUsername());

                throw new RuntimeException(
                        "Account locked until "
                                + user.getLockedUntil()
                );
            }

            log.info("[LOGIN] Lock period expired. Unlocking account. Username={}",
                    user.getUsername());

            user.setFailedAttempts(0);
            user.setAccountNonLocked(true);
            user.setLockedUntil(null);

            usersRepository.save(user);
            AuditLog auditLog = new AuditLog();

            auditLog.setIdentifier(user.getEmail());
            auditLog.setAction("ACCOUNT_UNLOCKED");
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDetails("Account automatically unlocked after lock period");

            auditLogRepository.save(auditLog);
        }
    }

    private void processFailedLogin(
            Users user,
            LoginRequest request,
            HttpServletRequest servletRequest) {

        log.warn("[LOGIN] Processing failed login. Username={}",
                user.getUsername());

        int attempts = user.getFailedAttempts() + 1;

        log.warn("[LOGIN] Failed login attempts updated. Username={}, Attempts={}",
                user.getUsername(),
                attempts);

        user.setFailedAttempts(attempts);

        boolean locked = false;

        if (attempts >= securityProperties.getMaxFailedAttempts()) {

            log.error("[LOGIN] Account locked after maximum failed attempts. Username={}",
                    user.getUsername());

            user.setAccountNonLocked(false);

            user.setLockedUntil(
                    LocalDateTime.now()
                            .plusMinutes(
                                    securityProperties.getLockTimeMinutes()
                            )
            );

            locked = true;
        }

        Users savedUser = usersRepository.save(user);

        log.info("[LOGIN] User account updated after failed login. UserId={}",
                user.getId());

        LoginAttempt attempt = new LoginAttempt();

        attempt.setUserId(user.getId());
        attempt.setEmail(user.getEmail());
        attempt.setPhoneNumber(user.getPhoneNumber());
        attempt.setLoginIdentifier(request.getIdentifier());

        attempt.setIdentifierType(
                getIdentifierType(
                        request.getIdentifier()
                )
        );

        attempt.setIpAddress(
                getClientIp(servletRequest)
        );

        attempt.setUserAgent(
                getDevice(servletRequest)
        );

        attempt.setAttemptStatus(
                AttemptStatus.FAILED
        );

        attempt.setFailureReason(
                locked
                        ? "ACCOUNT_LOCKED"
                        : "INVALID_PASSWORD"
        );

        attempt.setFailedAttempts(attempts);

        attempt.setAccountLocked(locked);

        if (locked) {
            attempt.setLockTime(
                    user.getLockedUntil()
            );
        }

        loginAttemptRepository.save(attempt);

        saveAuditLog(
                request.getIdentifier(),
                locked ? "ACCOUNT_LOCKED" : "LOGIN_FAILED",
                servletRequest,
                locked
                        ? "Account locked after maximum failed attempts"
                        : "Invalid password"
        );

        log.info("[LOGIN] Failed login attempt recorded. Identifier={}, Locked={}",
                request.getIdentifier(),
                locked);
    }

    private void resetFailedAttempts(
            Users user) {

        user.setFailedAttempts(0);
        user.setAccountNonLocked(true);
        user.setLockedUntil(null);
    }

    private void saveSuccessfulAttempt(
            Users user,
            String loginIdentifier,
            HttpServletRequest request) {

        log.warn("[LOGIN] Recording login attempt for unknown user. Identifier={}",
                loginIdentifier);
        LoginAttempt attempt = new LoginAttempt();


        attempt.setUserId(user.getId());

        attempt.setEmail(user.getEmail());

        attempt.setPhoneNumber(
                user.getPhoneNumber()
        );

        attempt.setLoginIdentifier(
                loginIdentifier
        );

        attempt.setIdentifierType(
                getIdentifierType(loginIdentifier)
        );


        attempt.setIpAddress(
                getClientIp(request)
        );

        attempt.setUserAgent(
                getDevice(request)
        );

        attempt.setAttemptStatus(
                AttemptStatus.SUCCESS
        );

        attempt.setFailedAttempts(0);

        attempt.setAccountLocked(false);

        loginAttemptRepository.save(attempt);

        log.info("[LOGIN] Successful login attempt saved. UserId={}",
                user.getId());


    }

    private void saveUnknownUserAttempt(
            String identifier,
            HttpServletRequest request,
            String reason) {

        LoginAttempt attempt = new LoginAttempt();
        attempt.setUserId(null);

        attempt.setLoginIdentifier(
                identifier
        );

        attempt.setIdentifierType(
                getIdentifierType(
                        identifier
                )
        );

        attempt.setIpAddress(
                getClientIp(request)
        );

        attempt.setUserAgent(
                getDevice(request)
        );

        attempt.setAttemptStatus(
                AttemptStatus.FAILED
        );

        attempt.setFailureReason(reason);

        loginAttemptRepository.save(attempt);
        log.info("[LOGIN] Unknown user login attempt saved.");
    }

    private String getIdentifierType(
            String identifier) {

        if (identifier.contains("@")) {
            return "EMAIL";
        }

        if (identifier.matches("\\d+")) {
            return "PHONE";
        }

        return "USERNAME";
    }

    private void saveAuditLog(
            String identifier,
            String action,
            HttpServletRequest request,
            String details) {

        AuditLog auditLog = new AuditLog();

        auditLog.setIdentifier(identifier);
        auditLog.setAction(action);

        auditLog.setIpAddress(
                getClientIp(request)
        );

        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(details);
        log.debug("[AUDIT] Saving audit log. Action={}, Identifier={}",
                action,
                identifier);
        auditLogRepository.save(auditLog);
        log.info("[AUDIT] Audit log saved successfully. Action={}",
                action);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()
                && !"unknown".equalsIgnoreCase(xfHeader)) {
            return xfHeader.split(",")[0];
        }

        String ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            try {
                ip = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                ip = "127.0.0.1";
            }
        }
        return ip;
    }

    private String getDevice(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return "UNKNOWN";
        }

        String browser = "Browser";
        String os = "OS";
        String deviceType = "Desktop";

        if (ua.contains("Edg")) {
            browser = "Edge";
        } else if (ua.contains("Chrome") && !ua.contains("Edg")) {
            browser = "Chrome";
        } else if (ua.contains("Firefox")) {
            browser = "Firefox";
        } else if (ua.contains("Safari") && !ua.contains("Chrome")) {
            browser = "Safari";
        }

        if (ua.contains("Windows")) {
            os = "Windows";
        } else if (ua.contains("Android")) {
            os = "Android";
        } else if (ua.contains("iPhone") || ua.contains("iOS")) {
            os = "iOS";
        } else if (ua.contains("Mac")) {
            os = "Mac";
        } else if (ua.contains("Linux")) {
            os = "Linux";
        }

        if (ua.contains("Mobile")) {
            deviceType = "Mobile";
        } else if (ua.contains("Tablet")) {
            deviceType = "Tablet";
        }

        return browser + " | " + os + " | " + deviceType;
    }

    /*-==============================================*/


    @Override
    public TokenResponse refreshToken(
            RefreshTokenRequest request,
            HttpServletRequest httpServletRequest) {

        log.info("[REFRESH_TOKEN] Token refresh process started.");

        String refreshToken = request.getRefreshToken();

        RefreshToken storedToken =
                refreshTokenRepository
                        .findByToken(refreshToken)
                        .orElseThrow(() -> {

                            log.warn("[REFRESH_TOKEN] Refresh token not found.");

                            return new RuntimeException(
                                    "Refresh token not found");
                        });

        log.debug(
                "[REFRESH_TOKEN] Refresh token located. TokenId={}, UserId={}",
                storedToken.getId(),
                storedToken.getUserId());

        if (storedToken.getRevoked()) {

            log.warn(
                    "[REFRESH_TOKEN] Attempt to use revoked refresh token. TokenId={}, UserId={}",
                    storedToken.getId(),
                    storedToken.getUserId());

            throw new RuntimeException("Refresh token revoked");
        }

        if (!jwtUtil.isTokenValid(refreshToken)) {

            log.warn(
                    "[REFRESH_TOKEN] Refresh token expired. TokenId={}, UserId={}",
                    storedToken.getId(),
                    storedToken.getUserId());

            throw new RuntimeException("Refresh token expired");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);

        log.debug(
                "[REFRESH_TOKEN] User ID extracted from refresh token. UserId={}",
                userId);

        Users user =
                usersRepository.findById(userId)
                        .orElseThrow(() -> {

                            log.warn(
                                    "[REFRESH_TOKEN] User not found. UserId={}",
                                    userId);

                            return new RuntimeException("User not found");
                        });

        log.debug(
                "[REFRESH_TOKEN] User located. UserId={}, Username={}",
                user.getId(),
                user.getUsername());

        // Revoke old refresh token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        log.info(
                "[REFRESH_TOKEN] Previous refresh token revoked. TokenId={}, UserId={}",
                storedToken.getId(),
                user.getId());

        // Generate new tokens
        String newAccessToken =
                jwtUtil.generateAccessToken(user);

        String newRefreshToken =
                jwtUtil.generateRefreshToken(user);

        log.debug(
                "[REFRESH_TOKEN] New JWT access and refresh tokens generated. UserId={}",
                user.getId());

        // Save new refresh token
        RefreshToken newToken =
                RefreshToken.builder()
                        .userId(user.getId())
                        .token(newRefreshToken)
                        .expiryDate(jwtUtil.extractExpiration(newRefreshToken))
                        .revoked(false)
                        .build();

        refreshTokenRepository.save(newToken);

        log.info(
                "[REFRESH_TOKEN] New refresh token saved. TokenId={}, UserId={}",
                newToken.getId(),
                user.getId());

        log.info(
                "[REFRESH_TOKEN] Token refresh completed successfully. UserId={}, Username={}",
                user.getId(),
                user.getUsername());

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

        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        Otp otp = Otp.builder()
                .user(user)
                .otpCode(otpCode)
                .purpose(OtpPurpose.FORGOT_PASSWORD)
                .verified(false)
                .email(user.getEmail())
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

        log.info("[LOGOUT] Logout process started.");

        // Blacklist access token
        tokenBlacklistService.blacklistToken(accessToken);

        log.debug("[LOGOUT] Access token blacklisted successfully.");

        // Find refresh token
        RefreshToken token = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> {

                    log.warn("[LOGOUT] Logout failed. Invalid refresh token.");

                    return new RuntimeException("Invalid refresh token");
                });

        log.debug(
                "[LOGOUT] Refresh token found. TokenId={}, UserId={}",
                token.getId(),
                token.getUserId());

        // Revoke refresh token
        token.setRevoked(true);

        refreshTokenRepository.save(token);

        log.info(
                "[LOGOUT] Refresh token revoked successfully. TokenId={}, UserId={}",
                token.getId(),
                token.getUserId());

        log.info(
                "[LOGOUT] Logout completed successfully. UserId={}",
                token.getUserId());

        return LogoutResponse.builder()
                .loggedOut(true)
                .tokenRevoked(true)
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {

        log.info("[VERIFY_EMAIL] Email verification started.");

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findByToken(token)
                        .orElseThrow(() -> {

                            log.warn("[VERIFY_EMAIL] Invalid verification token.");

                            return new RuntimeException(
                                    "Invalid verification token");
                        });

        log.debug("[VERIFY_EMAIL] Verification token found.");

        if (verificationToken.isUsed()) {

            log.warn("[VERIFY_EMAIL] Verification token already used.");

            throw new RuntimeException(
                    "Verification token already used");
        }

        log.debug("[VERIFY_EMAIL] Token has not been used.");

        if (verificationToken.getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            log.warn("[VERIFY_EMAIL] Verification token expired.");

            throw new RuntimeException(
                    "Verification token expired");
        }

        log.debug("[VERIFY_EMAIL] Token is valid.");

        Users user = verificationToken.getUser();

        log.debug("[VERIFY_EMAIL] Activating account. UserId={}, Username={}",
                user.getId(),
                user.getUsername());

        user.setEnabled(true);
        user.setEmailVerified(true);

        usersRepository.save(user);

        log.info("[VERIFY_EMAIL] User account activated. UserId={}, Username={}",
                user.getId(),
                user.getUsername());

        verificationToken.setUsed(true);

        emailVerificationTokenRepository.save(verificationToken);

        log.debug("[VERIFY_EMAIL] Verification token marked as used.");

        log.info("[VERIFY_EMAIL] Email verification completed successfully. UserId={}",
                user.getId());
    }

    @Override
    @Transactional
    public EmailOtpVerifyResponse verifyEmailPasswordOtp(
            VerifyEmailOtpRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        String otpCode = request.getOtp()
                .trim();

        Otp otpEntity = otpRepository
                .findByUserEmailAndOtpCodeAndPurpose(
                        email,
                        otpCode,
                        OtpPurpose.FORGOT_PASSWORD
                )
                .orElseThrow(() ->
                        new RuntimeException("Invalid OTP"));

        if (Boolean.TRUE.equals(otpEntity.getVerified())) {
            throw new RuntimeException("OTP already verified");
        }

        if (otpEntity.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException("OTP expired");
        }

        otpEntity.setVerified(true);

        otpRepository.save(otpEntity);

        return EmailOtpVerifyResponse.builder()
                .verified(true)
                .email(email)
                .canResetPassword(true)
                .build();
    }


    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(
            ResetPasswordRequest request,
            HttpServletRequest httpServletRequest) {

        String ip = getClientIp(httpServletRequest);
        String device = getDevice(httpServletRequest);

        // 1. GET VERIFIED OTP
        Otp savedOtp =
                otpRepository
                        .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                                request.getEmail(),
                                OtpPurpose.FORGOT_PASSWORD
                        )
                        .orElseThrow(() ->
                                new RuntimeException("OTP not found"));

        // 2. OTP VERIFIED CHECK
        if (!Boolean.TRUE.equals(savedOtp.getVerified())) {
            throw new RuntimeException(
                    "OTP verification required");
        }

        // 3. OTP EXPIRY CHECK
        if (savedOtp.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "OTP expired");
        }

        // 4. USER CHECK
        Users user =
                usersRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"));

        // 5. PASSWORD MATCH CHECK
        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new RuntimeException(
                    "Passwords do not match");
        }

        // 6. PREVENT CURRENT PASSWORD REUSE
        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "New password cannot be same as current password");
        }

        // 7. PASSWORD HISTORY CHECK
        int limit =
                securityProperties.getPasswordHistoryLimit();

//        List<PasswordHistory> lastPasswords =
//                passwordHistoryRepository
//                        .findTop5ByUserIdOrderByChangedAtDesc(
//                                user.getEmail());

        String userId = String.valueOf(user.getId());

        List<PasswordHistory> lastPasswords =
                passwordHistoryRepository
                        .findTop5ByUserIdOrderByChangedAtDesc(userId);

        for (PasswordHistory history :
                lastPasswords.stream()
                        .limit(limit)
                        .toList()) {

            if (passwordEncoder.matches(
                    request.getNewPassword(),
                    history.getPasswordHash())) {

                throw new RuntimeException(
                        "You cannot reuse last "
                                + limit
                                + " passwords");
            }
        }

        // 8. UPDATE PASSWORD
        String encodedPassword =
                passwordEncoder.encode(
                        request.getNewPassword());

        user.setPassword(encodedPassword);

        usersRepository.save(user);

        // 9. SAVE PASSWORD HISTORY
        PasswordHistory passwordHistory = new PasswordHistory();

        passwordHistory.setUserId(String.valueOf(user.getId()));
        passwordHistory.setPasswordHash(encodedPassword);

        passwordHistory.setChangedAt(LocalDateTime.now());

        passwordHistoryRepository.save(
                passwordHistory);

        // 10. REVOKE ALL ACTIVE REFRESH TOKENS
        List<RefreshToken> tokens =
                refreshTokenRepository
                        .findByUserIdAndRevokedFalse(
                                user.getId());

        tokens.forEach(token ->
                token.setRevoked(true));

        refreshTokenRepository.saveAll(
                tokens);

        // 11. MARK OTP AS CONSUMED
        savedOtp.setVerified(false);

        otpRepository.save(savedOtp);

        // 12. AUDIT LOG
        auditService.log(
                user.getEmail(),
                "PASSWORD_RESET",
                ip,
                device,
                "Password changed successfully"
        );

        // 13. RESPONSE
        return ResetPasswordResponse.builder()
                .passwordUpdated(true)
                .tokensRevoked(true)
                .build();
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {

        String token = resolveToken(httpRequest);
        Long userId = jwtUtil.extractUserId(token);

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // 2. New password should not match old password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password cannot be same as current password");
        }

        String uid = String.valueOf(user.getId());

        // 3. Password history validation
        validatePasswordHistory(uid, request.getNewPassword());

        // 4. Save current password to history
        savePasswordHistory(uid, user.getPassword());

        // 5. Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        usersRepository.save(user);

        return ChangePasswordResponse.builder()
                .message("Password changed successfully")
                .build();
    }
    @Override
    public List<LoginHistoryResponse> getLoginHistory(Long userId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        Long currentUserId =
                Long.parseLong(userDetails.getUsername());

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUserId.equals(userId)) {
            throw new RuntimeException(
                    "You can only view your own login history");
        }

        return loginAttemptRepository
                .findByUserIdOrderByAttemptTimeDesc(String.valueOf(userId))
                .stream()
                .map(attempt -> new LoginHistoryResponse(
                        attempt.getAttemptStatus().name(),
                        attempt.getIpAddress(),
                        attempt.getDeviceInfo(),
                        attempt.getAttemptTime()
                ))
                .toList();
    }
    @Override
    @Transactional
    public ResendVerificationEmailResponse resendVerificationEmail(String email) {

        log.info(
                "[RESEND_VERIFICATION_EMAIL] Resend verification email process started. Email={}",
                email);

        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> {

                    log.warn(
                            "[RESEND_VERIFICATION_EMAIL] User not found. Email={}",
                            email);

                    return new RuntimeException("User not found");
                });

        log.debug(
                "[RESEND_VERIFICATION_EMAIL] User located. UserId={}, Username={}",
                user.getId(),
                user.getUsername());

        // Email already verified
        if (user.isEmailVerified()) {

            log.warn(
                    "[RESEND_VERIFICATION_EMAIL] Email already verified. UserId={}, Email={}",
                    user.getId(),
                    user.getEmail());

            throw new RuntimeException("Email already verified");
        }

        // Maximum resend attempts
        long resendCount =
                emailVerificationTokenRepository.countByUserAndCreatedAtAfter(
                        user,
                        LocalDateTime.now().minusHours(1));

        log.debug(
                "[RESEND_VERIFICATION_EMAIL] Resend count in last hour={}. UserId={}",
                resendCount,
                user.getId());

        if (resendCount >= 3) {

            log.warn(
                    "[RESEND_VERIFICATION_EMAIL] Maximum resend attempts exceeded. UserId={}",
                    user.getId());

            throw new RuntimeException(
                    "Maximum resend attempts reached. Try again after 1 hour.");
        }

        // Cooldown check
        EmailVerificationToken latestToken =
                emailVerificationTokenRepository
                        .findTopByUserOrderByCreatedAtDesc(user)
                        .orElse(null);

        if (latestToken != null &&
                latestToken.getCreatedAt()
                        .plusSeconds(60)
                        .isAfter(LocalDateTime.now())) {

            log.warn(
                    "[RESEND_VERIFICATION_EMAIL] Cooldown period active. UserId={}",
                    user.getId());

            throw new RuntimeException(
                    "Please wait 60 seconds before requesting another verification email.");
        }

        log.debug(
                "[RESEND_VERIFICATION_EMAIL] Removing previous verification tokens. UserId={}",
                user.getId());

        emailVerificationTokenRepository.deleteByUser(user);
        emailVerificationTokenRepository.flush();

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .token(token)
                        .user(user)
                        .used(false)
                        .expiryDate(LocalDateTime.now().plusMinutes(15))
                        .build();

        emailVerificationTokenRepository.save(verificationToken);

        log.debug(
                "[RESEND_VERIFICATION_EMAIL] New verification token created. UserId={}",
                user.getId());

        emailService.sendVerificationEmail(
                user.getEmail(),
                token);

        log.info(
                "[RESEND_VERIFICATION_EMAIL] Verification email sent successfully. UserId={}, Email={}",
                user.getId(),
                user.getEmail());

        log.info(
                "[RESEND_VERIFICATION_EMAIL] Resend verification email process completed successfully. UserId={}",
                user.getId());

        return ResendVerificationEmailResponse.builder()
                .emailSent(true)
                .email(user.getEmail())
                .expiryMinutes(15)
                .build();
    }

    @Override
    @Transactional
    public OtpResponse resendPasswordOtp(String email) {

        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Delete previous forgot-password OTPs
        otpRepository.deleteByUserAndPurpose(
                user,
                OtpPurpose.FORGOT_PASSWORD
        );

        String otpCode = String.format(
                "%06d",
                ThreadLocalRandom.current()
                        .nextInt(100000, 1000000)
        );

        LocalDateTime expiryTime =
                LocalDateTime.now()
                        .plusMinutes(otpExpiryMinutes);

        Otp otp = Otp.builder()
                .user(user)
                .email(user.getEmail())
                .otpCode(otpCode)
                .purpose(OtpPurpose.FORGOT_PASSWORD)
                .verified(false)
                .attemptCount(0)
                .expiryTime(expiryTime)
                .build();

        otpRepository.save(otp);

        emailService.sendOtpEmail(
                user.getEmail(),
                otpCode
        );

        return OtpResponse.builder()
                .email(user.getEmail())
                .expiryMinutes(otpExpiryMinutes)
                .emailSent(true)
                .build();
    }

    private void savePasswordHistory(
            String userId,
            String passwordHash) {

        PasswordHistory history = new PasswordHistory();

        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        history.setChangedAt(LocalDateTime.now());

        passwordHistoryRepository.save(history);
    }

    private void validatePasswordHistory(String userId, String newPassword) {

        List<PasswordHistory> histories =
                passwordHistoryRepository
                        .findTop5ByUserIdOrderByChangedAtDesc(userId);

        for (PasswordHistory history : histories) {

            if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
                throw new RuntimeException(
                        "You cannot reuse your last " + passwordHistoryLimit + " passwords"
                );
            }
        }
    }
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        return header.substring(7);
    }
}

package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.config.SecurityProperties;
import com.cloudFileStorageSystem.dtos.request.*;
import com.cloudFileStorageSystem.dtos.response.*;
import com.cloudFileStorageSystem.enums.AttemptStatus;
import com.cloudFileStorageSystem.enums.OtpPurpose;
import com.cloudFileStorageSystem.module.*;
import com.cloudFileStorageSystem.repository.*;
import com.cloudFileStorageSystem.service.AuditService;
import com.cloudFileStorageSystem.service.AuthService;
import com.cloudFileStorageSystem.service.EmailService;
import com.cloudFileStorageSystem.service.TokenBlacklistService;
import com.cloudFileStorageSystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {


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

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    @Value("${security.password-history-limit}")
    private int passwordHistoryLimit;


    public AuthServiceImpl(PasswordHistoryRepository passwordHistoryRepository, PasswordResetOtpRepository passwordResetOtpRepository, SecurityProperties securityProperties, AuditLogRepository auditLogRepository, LoginAttemptRepository loginAttemptRepository, PasswordEncoder passwordEncoder, AuditService auditService, EmailVerificationTokenRepository emailVerificationTokenRepository, OtpRepository otpRepository, EmailService emailService, TokenBlacklistService tokenBlacklistService, RefreshTokenRepository refreshTokenRepository, UsersRepository usersRepository, JwtUtil jwtUtil) {
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
    }


    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {

        String identifier = request.getIdentifier();

        log.info("========== LOGIN START ==========");
        log.info("Identifier: {}", identifier);

        Users user = usersRepository
                .findByUsernameOrEmailOrPhoneNumber(
                        identifier,
                        identifier,
                        identifier
                )
                .orElseThrow(() -> {

                    log.warn("User NOT FOUND for identifier={}", identifier);

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
                "User Found -> username={}, email={}, failedAttempts={}, accountNonLocked={}, emailVerified={}, enabled={}",
                user.getUsername(),
                user.getEmail(),
                user.getFailedAttempts(),
                user.isAccountNonLocked(),
                user.isEmailVerified(),
                user.isEnabled()
        );

        validateAccountStatus(user);

        boolean passwordMatched =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        log.info("Password Match Result: {}", passwordMatched);

        if (!passwordMatched) {

            log.warn(
                    "Password mismatch for username={}",
                    user.getUsername()
            );

            processFailedLogin(
                    user,
                    request,
                    httpServletRequest
            );

            log.warn(
                    "Throwing Invalid Credentials Exception for username={}",
                    user.getUsername()
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

        log.info(
                "User updated after successful login. failedAttempts={}, locked={}",
                user.getFailedAttempts(),
                !user.isAccountNonLocked()
        );

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

        log.info(
                "Tokens generated successfully for username={}",
                user.getUsername()
        );

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

        log.info(
                "Login completed successfully for username={}",
                user.getUsername()
        );

        log.info("========== LOGIN END ==========");

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

        log.info(
                "Validating account status -> username={}, enabled={}, emailVerified={}, accountNonLocked={}, lockedUntil={}",
                user.getUsername(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.isAccountNonLocked(),
                user.getLockedUntil()
        );

        if (!user.isEnabled()) {
            log.error("Account Disabled");
            throw new RuntimeException("Account is disabled");
        }

        if (!user.isEmailVerified()) {
            log.error("Email Not Verified");
            throw new RuntimeException(
                    "Please verify your email first"
            );
        }

        if (!user.isAccountNonLocked()) {

            if (user.getLockedUntil() != null &&
                    user.getLockedUntil().isAfter(LocalDateTime.now())) {

                log.error(
                        "Account currently locked until {}",
                        user.getLockedUntil()
                );

                throw new RuntimeException(
                        "Account locked until "
                                + user.getLockedUntil()
                );
            }

            log.info(
                    "Lock expired. Unlocking account."
            );

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

        log.warn(
                "processFailedLogin START for username={}",
                user.getUsername()
        );

        int attempts = user.getFailedAttempts() + 1;

        log.warn(
                "Previous Attempts={}, New Attempts={}",
                user.getFailedAttempts(),
                attempts
        );

        user.setFailedAttempts(attempts);

        boolean locked = false;

        if (attempts >= securityProperties.getMaxFailedAttempts()) {

            log.error(
                    "Account LOCKED for username={}",
                    user.getUsername()
            );

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

        log.info(
                "User Saved -> failedAttempts={}, accountNonLocked={}, lockedUntil={}",
                savedUser.getFailedAttempts(),
                savedUser.isAccountNonLocked(),
                savedUser.getLockedUntil()
        );

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

        log.info(
                "LoginAttempt Saved -> identifier={}, attempts={}, locked={}",
                request.getIdentifier(),
                attempts,
                locked
        );

        log.warn(
                "processFailedLogin END for username={}",
                user.getUsername()
        );
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

        auditLogRepository.save(auditLog);
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

    @Override
    @Transactional
    public void verifyEmail(String token) {

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid verification token"));

        if (verificationToken.isUsed()) {

            throw new RuntimeException(
                    "Verification token already used");
        }

        if (verificationToken.getExpiryDate()
                .isBefore(java.time.LocalDateTime.now())) {

            throw new RuntimeException(
                    "Verification token expired"
            );
        }

        Users user = verificationToken.getUser();

        user.setEnabled(true);
        user.setEmailVerified(true);

        usersRepository.save(user);

        verificationToken.setUsed(true);

        emailVerificationTokenRepository.save(verificationToken);
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

        return loginAttemptRepository.findByUserIdOrderByAttemptTimeDesc(String.valueOf(userId))
                .stream()
                .map(attempt -> new LoginHistoryResponse(
                        attempt.getAttemptStatus().name(),
                        attempt.getIpAddress(),
                        attempt.getDeviceInfo(),
                        attempt.getAttemptTime()
                ))
                .collect(Collectors.toList());
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

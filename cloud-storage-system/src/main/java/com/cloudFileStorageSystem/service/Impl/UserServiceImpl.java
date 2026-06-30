package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.dtos.request.UserRegistrationRequest;
import com.cloudFileStorageSystem.dtos.response.UserResponse;
import com.cloudFileStorageSystem.enums.Role;
import com.cloudFileStorageSystem.exception.ResourceAlreadyExistsException;
import com.cloudFileStorageSystem.module.EmailVerificationToken;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.EmailVerificationTokenRepository;
import com.cloudFileStorageSystem.repository.UsersRepository;
import com.cloudFileStorageSystem.service.EmailService;
import com.cloudFileStorageSystem.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;
    private final EmailService emailService;

    private static final Logger log =
            LoggerFactory.getLogger(UserServiceImpl.class);


    public UserServiceImpl(EmailVerificationTokenRepository emailVerificationTokenRepository, PasswordEncoder passwordEncoder, UsersRepository usersRepository, EmailService emailService) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.usersRepository = usersRepository;
        this.emailService = emailService;
    }


    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {

        log.info("[REGISTER_USER] Registration started.");

        log.debug("[REGISTER_USER] Checking username availability.");

        if (usersRepository.existsByUsername(request.getUsername())) {

            log.warn("[REGISTER_USER] Username already exists | username={}",
                    request.getUsername());

            throw new ResourceAlreadyExistsException(
                    "Username '" + request.getUsername() + "' already exists");
        }

        log.debug("[REGISTER_USER] Username validation passed.");

        log.debug("[REGISTER_USER] Checking email availability.");

        if (usersRepository.existsByEmail(request.getEmail())) {

            log.warn("[REGISTER_USER] Email already exists | email={}", request.getEmail());

            throw new ResourceAlreadyExistsException(
                    "Email '" + request.getEmail() + "' already exists");
        }

        log.debug("[REGISTER_USER] Email validation passed.");

        log.debug("[REGISTER_USER] Encrypting password.");

        Users user = Users.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.USER)
                .enabled(true)
                .emailVerified(false)
                .build();

        log.debug("[REGISTER_USER] User entity created.");

        Users savedUser = usersRepository.save(user);

        log.info("[REGISTER_USER] User saved successfully | userId={}", savedUser.getId());

        String token = UUID.randomUUID().toString();

        log.debug("[REGISTER_USER] Verification token generated.");

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .token(token)
                        .user(savedUser)
                        .expiryDate(LocalDateTime.now().plusMinutes(15))
                        .used(false)
                        .resendCount(0)
                        .build();

        emailVerificationTokenRepository.save(verificationToken);

        log.debug("[REGISTER_USER] Verification token saved.");


        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        log.info("[REGISTER_USER] Verification email sent | email={}",
                savedUser.getEmail());


        log.info("[REGISTER_USER] Registration completed successfully | userId={} | username={}",
                savedUser.getId(),
                savedUser.getUsername());

        return mapToResponse(savedUser);
    }

    private UserResponse mapToResponse(Users user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }
}

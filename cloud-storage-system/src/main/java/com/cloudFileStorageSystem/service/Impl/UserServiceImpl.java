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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;
    private final EmailService emailService;

    public UserServiceImpl(EmailVerificationTokenRepository emailVerificationTokenRepository, PasswordEncoder passwordEncoder, UsersRepository usersRepository, EmailService emailService) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.usersRepository = usersRepository;
        this.emailService = emailService;
    }


    @Override
    public UserResponse registerUser(UserRegistrationRequest userRegistrationRequest) {
        log.info("Starting user registration. Username={}, Email={}", userRegistrationRequest.getUsername(), userRegistrationRequest.getEmail());

        if (usersRepository.existsByUsername(
                userRegistrationRequest.getUsername())) {

            log.warn("Registration failed. Username already exists: {}",
                    userRegistrationRequest.getUsername());

            throw new ResourceAlreadyExistsException(
                    "Username '" +
                            userRegistrationRequest.getUsername() +
                            "' already exists");
        }

        if (usersRepository.existsByEmail(
                userRegistrationRequest.getEmail())) {

            log.warn("Registration failed. Email already exists: {}",
                    userRegistrationRequest.getEmail());

            throw new ResourceAlreadyExistsException(
                    "Email '" +
                            userRegistrationRequest.getEmail() +
                            "' already exists");
        }

        log.debug("Username and email validation passed");

        Users users = Users.builder()
                .firstName(userRegistrationRequest.getFirstName())
                .lastName(userRegistrationRequest.getLastName())
                .username(userRegistrationRequest.getUsername())
                .email(userRegistrationRequest.getEmail())
                .password(passwordEncoder.encode(
                        userRegistrationRequest.getPassword()))
                .phoneNumber(userRegistrationRequest.getPhoneNumber())
                .role(Role.USER)
                .emailVerified(false)
                .enabled(true)
                .build();

        log.debug("User entity created. Username={}",
                users.getUsername());

        Users savedUser = usersRepository.save(users);

        String emailToken = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .token(emailToken)
                        .user(savedUser)
                        .expiryDate(LocalDateTime.now().plusMinutes(15))
                        .used(false)
                        .resendCount(0)
                        .build();

        emailVerificationTokenRepository .save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(
                savedUser.getEmail(),
                emailToken
        );

        log.info("User registered successfully. UserId={}, Username={}, Role={}",
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole());

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

package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.dtos.request.UserRegistrationRequest;
import com.cloudFileStorageSystem.dtos.response.UserResponse;
import com.cloudFileStorageSystem.enums.Role;
import com.cloudFileStorageSystem.exception.ResourceAlreadyExistsException;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.UsersRepository;
import com.cloudFileStorageSystem.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;

    public UserServiceImpl(PasswordEncoder passwordEncoder, UsersRepository usersRepository) {
        this.passwordEncoder = passwordEncoder;
        this.usersRepository = usersRepository;
    }


    @Override
    public UserResponse registerUser(UserRegistrationRequest userRegistrationRequest) {

        if (usersRepository.existsByUsername(userRegistrationRequest.getUsername())) {
            throw new ResourceAlreadyExistsException(
                    "Username '" + userRegistrationRequest.getUsername() + "' already exists");
        }

        if (usersRepository.existsByEmail(userRegistrationRequest.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "Email '" + userRegistrationRequest.getEmail() + "' already exists");
        }
        Users users = Users.builder()
                .firstName(userRegistrationRequest.getFirstName())
                .lastName(userRegistrationRequest.getLastName())
                .username(userRegistrationRequest.getUsername())
                .email(userRegistrationRequest.getEmail())
                .password(passwordEncoder.encode(userRegistrationRequest.getPassword()))
                .phoneNumber(userRegistrationRequest.getPhoneNumber())
                .role(Role.USER)
                .emailVerified(false)
                .enabled(true)
                .build();

        Users savedUser = usersRepository.save(users);

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
                .emailVerified(user.getEmailVerified())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();
    }
}

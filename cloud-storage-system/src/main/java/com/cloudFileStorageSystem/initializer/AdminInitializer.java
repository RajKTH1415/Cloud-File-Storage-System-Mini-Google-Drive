package com.cloudFileStorageSystem.initializer;

import com.cloudFileStorageSystem.enums.Role;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.UsersRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void createAdmin() {

        System.out.println("AdminInitializer Started");

        if (!usersRepository.existsByEmail("admin@cloudstorage.com")) {

            System.out.println("Creating Admin User...");

            Users admin = Users.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .username("admin")
                    .email("admin@cloudstorage.com")
                    .password("Admin@123")
                    .role(Role.ADMIN)
                    .emailVerified(true)
                    .enabled(true)
                    .build();

            usersRepository.save(admin);

            System.out.println("Admin Created Successfully");
        }
    }
}
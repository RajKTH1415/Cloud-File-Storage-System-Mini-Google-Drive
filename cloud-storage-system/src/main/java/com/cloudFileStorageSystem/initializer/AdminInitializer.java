package com.cloudFileStorageSystem.initializer;

import com.cloudFileStorageSystem.enums.Role;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    private final UsersRepository usersRepository;

    public AdminInitializer(PasswordEncoder passwordEncoder, UsersRepository usersRepository) {
        this.passwordEncoder = passwordEncoder;
        this.usersRepository = usersRepository;
    }

    @Override
    public void run(String @NonNull ... args) {

        log.info("AdminInitializer Started");

        if (!usersRepository.existsByEmail("admin@cloudstorage.com")) {

            Users admin = Users.builder()
                    .firstName("ADMIN")
                    .lastName("Admin")
                    .username("admin")
                    .email("admin@cloudstorage.com")
                    .password(
                            passwordEncoder.encode("Admin@123")
                    )
                    .phoneNumber("9931522260")
                    .role(Role.ADMIN)
                    .emailVerified(true)
                    .enabled(true)
                    .build();

            usersRepository.save(admin);

            log.info("Admin Created Successfully");
        }
    }
}
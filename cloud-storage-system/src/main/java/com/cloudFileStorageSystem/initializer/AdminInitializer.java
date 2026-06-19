package com.cloudFileStorageSystem.initializer;

import com.cloudFileStorageSystem.enums.Role;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UsersRepository usersRepository;

    @Override
    public void run(String... args) {

        log.info("AdminInitializer Started");

        if (!usersRepository.existsByEmail("admin@cloudstorage.com")) {

            Users admin = Users.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .username("admin")
                    .email("admin@cloudstorage.com")
                    .password("Admin@123")
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
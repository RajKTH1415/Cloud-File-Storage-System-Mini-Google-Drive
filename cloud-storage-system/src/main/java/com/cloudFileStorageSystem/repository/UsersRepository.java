package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.enums.Role;
import com.cloudFileStorageSystem.module.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    Optional<Users> findByUsername(String username);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByPhoneNumber(String phoneNumber);

    Optional<Users> findByUsernameOrEmailOrPhoneNumber(
            String username,
            String email,
            String phoneNumber
    );
}
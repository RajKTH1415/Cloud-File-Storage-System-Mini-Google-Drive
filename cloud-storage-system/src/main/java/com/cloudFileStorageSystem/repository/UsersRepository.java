package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.enums.UserStatus;
import com.cloudFileStorageSystem.module.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<Users> findByEmail(String email);


    Optional<Users> findByUsernameOrEmailOrPhoneNumber(
            String username,
            String email,
            String phoneNumber
    );

    @Query("SELECT COUNT(u) FROM Users u WHERE u.status = :status")
    long countByStatus(UserStatus status);
}
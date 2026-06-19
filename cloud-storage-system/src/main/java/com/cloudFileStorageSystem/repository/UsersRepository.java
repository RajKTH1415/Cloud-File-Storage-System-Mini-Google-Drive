package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.enums.Role;
import com.cloudFileStorageSystem.module.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, Long> {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByRole(Role role);
}

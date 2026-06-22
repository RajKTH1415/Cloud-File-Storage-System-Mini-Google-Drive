package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.enums.OtpPurpose;
import com.cloudFileStorageSystem.module.Otp;
import com.cloudFileStorageSystem.module.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByUserAndPurposeOrderByIdDesc(
            Users user,
            OtpPurpose purpose
    );

    void deleteByUserAndPurpose(
            Users user,
            OtpPurpose purpose
    );
}

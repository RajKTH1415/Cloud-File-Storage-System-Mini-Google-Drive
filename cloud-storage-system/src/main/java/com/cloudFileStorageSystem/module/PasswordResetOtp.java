package com.cloudFileStorageSystem.module;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_otp")
@Data
public class PasswordResetOtp extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "expiry_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiryTime;
}
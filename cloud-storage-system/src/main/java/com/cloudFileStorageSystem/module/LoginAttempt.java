package com.cloudFileStorageSystem.module;

import com.cloudFileStorageSystem.enums.AttemptStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "login_identifier")
    private String loginIdentifier;

    @Column(name = "identifier_type")
    private String identifierType;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "device_info")
    private String deviceInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_status")
    private AttemptStatus attemptStatus;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "failed_attempts")
    private Integer failedAttempts;

    @Column(name = "account_locked")
    private boolean accountLocked;

    @Column(name = "lock_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockTime;

    @Column(name = "attempt_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime attemptTime;

    @PrePersist
    public void onCreate() {
        attemptTime = LocalDateTime.now();
    }
}
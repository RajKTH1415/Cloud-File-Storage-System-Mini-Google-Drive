package com.cloudFileStorageSystem.module;

import com.cloudFileStorageSystem.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Otp extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Long id;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "is_verified", nullable = false)
    private Boolean verified;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_otp_user")
    )
    private Users user;

    @PrePersist
    public void prePersist() {

        if (verified == null) {
            verified = false;
        }

        if (attemptCount == null) {
            attemptCount = 0;
        }
    }
}
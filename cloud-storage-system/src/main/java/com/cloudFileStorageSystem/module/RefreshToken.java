package com.cloudFileStorageSystem.module;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 1000)
    private String token;

    @Column(nullable = false)
    private Date expiryDate;

    @Column(nullable = false)
    private Boolean revoked;
}

package com.cloudFileStorageSystem.dtos.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockUserResponse {

    private Long userId;
    private String username;
    private String email;
    private boolean accountNonLocked;
    private LocalDateTime lockedUntil;
    private LocalDateTime lockedAt;
}
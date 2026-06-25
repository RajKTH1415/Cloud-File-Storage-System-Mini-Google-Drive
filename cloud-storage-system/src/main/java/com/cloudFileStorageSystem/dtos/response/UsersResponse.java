package com.cloudFileStorageSystem.dtos.response;

import com.cloudFileStorageSystem.enums.Role;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsersResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String username;

    private String email;

    private String phoneNumber;

    private Role role;

    private boolean enabled;

    private boolean emailVerified;

    private boolean accountNonLocked;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
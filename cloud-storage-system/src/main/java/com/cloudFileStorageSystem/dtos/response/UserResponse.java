package com.cloudFileStorageSystem.dtos.response;

import com.cloudFileStorageSystem.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private Boolean emailVerified;
}

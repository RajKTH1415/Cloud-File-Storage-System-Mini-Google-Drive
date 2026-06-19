package com.cloudFileStorageSystem.dtos.response;

import com.cloudFileStorageSystem.enums.Role;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({
        "id",
        "firstName",
        "lastName",
        "username",
        "email",
        "role",
        "emailVerified",
        "createdBy",
        "updatedBy"

})
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private Boolean emailVerified;
    private String createdBy;
    private String updatedBy;
}

package com.cloudFileStorageSystem.dtos.request;

import com.cloudFileStorageSystem.enums.Role;
import lombok.Data;

@Data
public class UserRegistrationRequest {

    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
}

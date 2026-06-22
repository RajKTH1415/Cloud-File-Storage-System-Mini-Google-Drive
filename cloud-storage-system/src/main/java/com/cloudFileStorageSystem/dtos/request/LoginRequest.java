package com.cloudFileStorageSystem.dtos.request;

import lombok.Data;

@Data
public class LoginRequest {

    private String identifier; // username OR email

    private String password;
}
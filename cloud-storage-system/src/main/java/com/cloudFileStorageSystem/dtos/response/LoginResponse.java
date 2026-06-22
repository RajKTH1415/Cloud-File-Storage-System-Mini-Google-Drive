package com.cloudFileStorageSystem.dtos.response;

import com.cloudFileStorageSystem.module.TokenData;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({
        "username",
        "fullName",
        "email",
        "phoneNumber",
        "tokenData"

})
public class LoginResponse {

    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private TokenData tokens;
}
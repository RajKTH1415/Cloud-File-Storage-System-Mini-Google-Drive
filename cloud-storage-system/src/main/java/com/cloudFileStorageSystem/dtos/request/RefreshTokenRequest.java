package com.cloudFileStorageSystem.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    private String accessToken;
    private String refreshToken;
}

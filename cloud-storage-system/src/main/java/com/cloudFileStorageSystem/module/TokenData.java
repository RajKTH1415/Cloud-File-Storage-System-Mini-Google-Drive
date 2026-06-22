package com.cloudFileStorageSystem.module;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class TokenData {

    private String accessToken;
    private String refreshToken;

    // builder, getters
}
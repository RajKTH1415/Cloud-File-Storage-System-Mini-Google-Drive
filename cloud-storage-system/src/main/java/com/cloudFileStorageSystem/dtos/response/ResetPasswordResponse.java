package com.cloudFileStorageSystem.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResetPasswordResponse {
    private boolean passwordUpdated;
    private boolean tokensRevoked;
}

package com.cloudFileStorageSystem.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangePasswordResponse {

    private String message;
}
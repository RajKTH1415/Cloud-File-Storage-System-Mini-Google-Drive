package com.cloudFileStorageSystem.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class EmailOtpVerifyResponse {

    private boolean verified;
    private String email;
    private boolean canResetPassword;
}
package com.cloudFileStorageSystem.dtos.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@JsonPropertyOrder({
        "expiryMinutes",
        "emailSent"
})
@Data
@Builder
public class OtpResponse {

    private int expiryMinutes;
    private boolean emailSent;
    private String email;
}
package com.cloudFileStorageSystem.dtos.response;


import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResendVerificationEmailResponse {

    private boolean emailSent;
    private String email;
    private int expiryMinutes;
}

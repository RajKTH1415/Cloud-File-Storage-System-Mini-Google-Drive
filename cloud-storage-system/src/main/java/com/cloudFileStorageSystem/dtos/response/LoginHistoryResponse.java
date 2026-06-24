package com.cloudFileStorageSystem.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginHistoryResponse {
    private String status;
    private String ip;
    private String device;
    private LocalDateTime time;
}
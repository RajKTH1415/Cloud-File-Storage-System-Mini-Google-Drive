package com.cloudFileStorageSystem.dtos.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;

    private Long userId;

    private String identifier;

    private String action;

    private String details;

    private LocalDateTime timestamp;
}
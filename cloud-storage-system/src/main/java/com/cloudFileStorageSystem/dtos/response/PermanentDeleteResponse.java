package com.cloudFileStorageSystem.dtos.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermanentDeleteResponse {

    private Long fileId;

    private String fileName;

    private LocalDateTime deletedAt;

    private String message;
}
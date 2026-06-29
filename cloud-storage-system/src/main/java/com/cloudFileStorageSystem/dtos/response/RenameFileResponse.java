package com.cloudFileStorageSystem.dtos.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenameFileResponse {

    private Long fileId;

    private String oldFileName;

    private String newFileName;

    private LocalDateTime updatedAt;
}
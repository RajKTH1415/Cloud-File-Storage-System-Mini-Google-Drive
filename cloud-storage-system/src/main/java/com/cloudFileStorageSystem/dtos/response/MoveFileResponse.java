package com.cloudFileStorageSystem.dtos.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveFileResponse {

    private Long fileId;

    private String fileName;

    private String oldFolder;

    private String newFolder;

    private LocalDateTime updatedAt;

}
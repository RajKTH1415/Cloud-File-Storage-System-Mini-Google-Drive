package com.cloudFileStorageSystem.dtos.response;

import com.cloudFileStorageSystem.enums.FileStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteFileResponse {

    private Long fileId;
    private String fileName;
    private FileStatus status;
    private LocalDateTime deletedAt;
}
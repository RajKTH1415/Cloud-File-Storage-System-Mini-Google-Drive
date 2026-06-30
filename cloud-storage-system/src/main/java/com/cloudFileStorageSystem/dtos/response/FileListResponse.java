package com.cloudFileStorageSystem.dtos.response;

import com.cloudFileStorageSystem.enums.FileStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileListResponse {

    private Long fileId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String folderName;

    private Boolean isPublic;

    private FileStatus status;

    private LocalDateTime uploadedAt;
}
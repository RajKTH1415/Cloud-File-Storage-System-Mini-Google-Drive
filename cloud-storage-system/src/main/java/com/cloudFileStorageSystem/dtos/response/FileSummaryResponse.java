package com.cloudFileStorageSystem.dtos.response;

import com.cloudFileStorageSystem.enums.FileStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileSummaryResponse {

    private Long fileId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String folderName;
    private Boolean isPublic;
    private FileStatus status;
    private LocalDateTime uploadedAt;
}
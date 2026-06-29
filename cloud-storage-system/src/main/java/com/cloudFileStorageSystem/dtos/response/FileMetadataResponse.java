package com.cloudFileStorageSystem.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FileMetadataResponse {

    private Long fileId;
    private String fileName;
    private String fileType;
    private Long size;
    private String downloadUrl;
}
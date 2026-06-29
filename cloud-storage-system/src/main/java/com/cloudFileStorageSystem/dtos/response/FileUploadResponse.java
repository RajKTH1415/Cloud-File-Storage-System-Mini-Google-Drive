package com.cloudFileStorageSystem.dtos.response;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FileUploadResponse {

    private Long id;

    private String originalName;

    private String fileType;

    private String fileExtension;

    private Long fileSize;

    private String folderName;

    private Boolean isPublic;

    private String message;
}
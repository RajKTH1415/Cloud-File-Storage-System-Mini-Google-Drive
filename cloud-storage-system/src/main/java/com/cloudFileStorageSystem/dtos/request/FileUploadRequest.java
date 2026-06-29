package com.cloudFileStorageSystem.dtos.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadRequest {

    @Size(max = 255)
    private String folderName;

    private Boolean isPublic = false;
}
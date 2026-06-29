package com.cloudFileStorageSystem.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenameFileRequest {

    @NotBlank(message = "File name is required.")
    @Size(max = 255, message = "File name must not exceed 255 characters.")
    private String fileName;
}
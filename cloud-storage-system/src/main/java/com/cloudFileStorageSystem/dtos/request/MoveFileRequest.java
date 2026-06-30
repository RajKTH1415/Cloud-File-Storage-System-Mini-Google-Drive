package com.cloudFileStorageSystem.dtos.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveFileRequest {

    @NotBlank(message = "Folder name is required.")
    @Size(max = 255, message = "Folder name cannot exceed 255 characters.")
    private String folderName;

}
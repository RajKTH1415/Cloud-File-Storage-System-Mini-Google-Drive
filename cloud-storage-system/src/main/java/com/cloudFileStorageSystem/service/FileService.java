package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.request.FileUploadRequest;
import com.cloudFileStorageSystem.dtos.response.FileUploadResponse;
import com.cloudFileStorageSystem.module.Users;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileUploadResponse uploadFile(
            MultipartFile file,
            FileUploadRequest request,
            Long userId
    );
}

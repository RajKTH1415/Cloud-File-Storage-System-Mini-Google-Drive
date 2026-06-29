package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.request.FileUploadRequest;
import com.cloudFileStorageSystem.dtos.response.FileUploadResponse;
import com.cloudFileStorageSystem.module.FileEntity;
import com.cloudFileStorageSystem.module.Users;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileUploadResponse uploadFile(
            MultipartFile file,
            FileUploadRequest request,
            Long userId
    );

    FileEntity getFileById(Long fileId);

    ResponseEntity<Resource> downloadFile(Long fileId);
}

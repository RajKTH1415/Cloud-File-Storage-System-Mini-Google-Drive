package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.request.FileUploadRequest;
import com.cloudFileStorageSystem.dtos.request.MoveFileRequest;
import com.cloudFileStorageSystem.dtos.request.RenameFileRequest;
import com.cloudFileStorageSystem.dtos.response.*;
import com.cloudFileStorageSystem.module.FileEntity;
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

    DeleteFileResponse deleteFile(Long fileId);

    RenameFileResponse renameFile(
            Long fileId,
            RenameFileRequest request
    );

    PageResponse<FileSummaryResponse> getMyFiles(
            int page,
            int size,
            String sortBy,
            String direction
    );

    PageResponse<FileSummaryResponse> getTrashFiles(
            int page,
            int size,
            String sortBy,
            String direction
    );

    RestoreFileResponse restoreFile(Long fileId);

    PermanentDeleteResponse permanentDeleteFile(Long fileId);


    MoveFileResponse moveFile(Long fileId, MoveFileRequest request);
}

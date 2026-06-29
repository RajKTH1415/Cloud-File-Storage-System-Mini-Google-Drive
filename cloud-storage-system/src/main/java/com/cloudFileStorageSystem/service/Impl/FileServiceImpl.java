package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.dtos.request.FileUploadRequest;
import com.cloudFileStorageSystem.dtos.response.FileUploadResponse;
import com.cloudFileStorageSystem.enums.FileCategory;
import com.cloudFileStorageSystem.enums.FileStatus;
import com.cloudFileStorageSystem.module.FileEntity;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.FileRepository;
import com.cloudFileStorageSystem.repository.UsersRepository;
import com.cloudFileStorageSystem.service.FileService;
import com.cloudFileStorageSystem.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final UsersRepository usersRepository;
    private final FileRepository fileRepository;
    private final StorageService storageService;

    public FileServiceImpl(UsersRepository usersRepository,
                           FileRepository fileRepository,
                           StorageService storageService) {
        this.usersRepository = usersRepository;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public FileUploadResponse uploadFile(
            MultipartFile file,
            FileUploadRequest request,
            Long userId
    ) {

        log.info("[FILE_UPLOAD] Upload processing started. UserId={}, FileName={}",
                userId,
                file.getOriginalFilename());

        try {

            Users owner = usersRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("[FILE_UPLOAD] User not found. UserId={}", userId);
                        return new RuntimeException("User not found");
                    });

            log.info("[FILE_UPLOAD] User validated successfully. UserId={}",
                    owner.getId());

            log.info("[FILE_UPLOAD] Storing file. FileName={}",
                    file.getOriginalFilename());

            String storagePath = storageService.store(file, owner.getId());

            log.info("[FILE_UPLOAD] File stored successfully. StoragePath={}",
                    storagePath);

            FileEntity fileEntity = FileEntity.builder()
                    .originalName(file.getOriginalFilename())
                    .storedName(Path.of(storagePath).getFileName().toString())
                    .fileType(file.getContentType())
                    .fileExtension(getExtension(file.getOriginalFilename()))
                    .fileSize(file.getSize())
                    .storagePath(storagePath)
                    .folderName(request.getFolderName())
                    .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                    .deleted(false)
                    .status(FileStatus.ACTIVE)
                    .category(FileCategory.OTHER)
                    .owner(owner)
                    .build();

            log.info("[FILE_UPLOAD] Saving file metadata.");

            fileEntity = fileRepository.save(fileEntity);

            log.info("[FILE_UPLOAD] Metadata saved successfully. FileId={}, UserId={}",
                    fileEntity.getId(),
                    owner.getId());

            FileUploadResponse response = FileUploadResponse.builder()
                    .id(fileEntity.getId())
                    .originalName(fileEntity.getOriginalName())
                    .fileType(fileEntity.getFileType())
                    .fileExtension(fileEntity.getFileExtension())
                    .fileSize(fileEntity.getFileSize())
                    .folderName(fileEntity.getFolderName())
                    .isPublic(fileEntity.getIsPublic())
                    .message("File uploaded successfully")
                    .build();

            log.info("[FILE_UPLOAD] Upload completed successfully. FileId={}, UserId={}",
                    fileEntity.getId(),
                    owner.getId());

            return response;

        } catch (Exception ex) {

            log.error("[FILE_UPLOAD] Upload failed. UserId={}, FileName={}, Error={}",
                    userId,
                    file.getOriginalFilename(),
                    ex.getMessage(),
                    ex);

            throw ex;
        }
    }

    private String getExtension(String fileName) {

        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
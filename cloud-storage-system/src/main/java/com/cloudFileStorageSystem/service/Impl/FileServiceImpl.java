package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.dtos.request.FileUploadRequest;
import com.cloudFileStorageSystem.dtos.request.RenameFileRequest;
import com.cloudFileStorageSystem.dtos.response.DeleteFileResponse;
import com.cloudFileStorageSystem.dtos.response.FileUploadResponse;
import com.cloudFileStorageSystem.dtos.response.RenameFileResponse;
import com.cloudFileStorageSystem.enums.FileCategory;
import com.cloudFileStorageSystem.enums.FileStatus;
import com.cloudFileStorageSystem.module.FileEntity;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.FileRepository;
import com.cloudFileStorageSystem.repository.UsersRepository;
import com.cloudFileStorageSystem.security.CustomUserDetailsService;
import com.cloudFileStorageSystem.security.CustomUserPrincipal;
import com.cloudFileStorageSystem.service.FileService;
import com.cloudFileStorageSystem.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

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
                    .isDeleted(false)
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

    @Override
    public FileEntity getFileById(Long fileId) {
        return validateFile(fileId);
    }


    @Override
    public ResponseEntity<Resource> downloadFile(Long fileId) {

        FileEntity file = validateFile(fileId);

        Path path = Paths.get(file.getStoragePath());

        if (!Files.exists(path)) {
            throw new RuntimeException("Physical file not found.");
        }

        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to load file.");
        }

        String contentType;
        try {
            contentType = Files.probeContentType(path);
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(file.getFileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalName() + "\""
                )
                .body(resource);
    }

    @Override
    @Transactional
    public DeleteFileResponse deleteFile(Long fileId) {

        FileEntity file = validateFile(fileId);

        Path path = Paths.get(file.getStoragePath());

        try {
            System.out.println("Deleting file: " + path);
            System.out.println("Exists before delete: " + Files.exists(path));

            boolean deleted = Files.deleteIfExists(path);

            System.out.println("Deleted: " + deleted);
            System.out.println("Exists after delete: " + Files.exists(path));
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete file.");
        }

        file.setIsDeleted(true);
        file.setStatus(FileStatus.DELETED);

        fileRepository.save(file);

        return DeleteFileResponse.builder()
                .fileId(file.getId())
                .fileName(file.getOriginalName())
                .status(FileStatus.DELETED)
                .deletedAt(file.getDeletedAt())
                .build();
    }

    @Override
    @Transactional
    public RenameFileResponse renameFile(
            Long fileId,
            RenameFileRequest request) {

        FileEntity file = validateFile(fileId);

        String oldFileName = file.getOriginalName();
        String extension = file.getFileExtension();

        String newFileName = request.getFileName().trim();

        // Preserve original extension
        if (!newFileName.toLowerCase().endsWith("." + extension.toLowerCase())) {
            newFileName = newFileName + "." + extension;
        }

        // Prevent renaming to the same name
        if (oldFileName.equalsIgnoreCase(newFileName)) {
            throw new RuntimeException("File already has this name.");
        }

        // Prevent duplicate filename in the same folder
        boolean exists = fileRepository
                .existsByOwnerIdAndFolderNameAndOriginalNameAndIsDeletedFalse(
                        file.getOwner().getId(),
                        file.getFolderName(),
                        newFileName
                );

        if (exists) {
            throw new RuntimeException(
                    "A file with the same name already exists in this folder."
            );
        }

        file.setOriginalName(newFileName);

        file = fileRepository.save(file);

        return RenameFileResponse.builder()
                .fileId(file.getId())
                .oldFileName(oldFileName)
                .newFileName(file.getOriginalName())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    private FileEntity validateFile(Long fileId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserPrincipal principal =
                (CustomUserPrincipal) authentication.getPrincipal();

        Long currentUserId = principal.getId();

        FileEntity file = fileRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getOwner().getId().equals(currentUserId)) {
            throw new RuntimeException("You are not allowed to access this file.");
        }

        return file;
    }
    private String getExtension(String fileName) {

        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
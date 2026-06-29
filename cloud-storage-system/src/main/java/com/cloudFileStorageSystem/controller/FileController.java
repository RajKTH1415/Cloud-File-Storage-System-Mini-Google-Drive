package com.cloudFileStorageSystem.controller;

import com.cloudFileStorageSystem.dtos.request.FileUploadRequest;
import com.cloudFileStorageSystem.dtos.request.RenameFileRequest;
import com.cloudFileStorageSystem.dtos.response.*;
import com.cloudFileStorageSystem.module.FileEntity;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.security.CustomUserDetailsService;
import com.cloudFileStorageSystem.security.CustomUserPrincipal;
import com.cloudFileStorageSystem.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(

            @RequestPart("file") MultipartFile file,

            @ModelAttribute FileUploadRequest request,

            Authentication authentication,

            HttpServletRequest requestHttp
    ) {

        CustomUserPrincipal principal =
                (CustomUserPrincipal) authentication.getPrincipal();

        Long userId = principal.getId();
        log.info(
                "[FILE_UPLOAD] Upload request received. UserId={}, FileName={}, Size={} bytes, Folder={}, IP={}, URI={}",
                userId,
                file.getOriginalFilename(),
                file.getSize(),
                request.getFolderName(),
                requestHttp.getRemoteAddr(),
                requestHttp.getRequestURI()
        );

        FileUploadResponse response =
                fileService.uploadFile(file, request, userId);

        log.info(
                "[FILE_UPLOAD] File uploaded successfully. UserId={}, FileId={}, FileName={}",
                userId,
                response.getId(),
                response.getOriginalName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "File uploaded successfully",
                        requestHttp.getRequestURI(),
                        response
                ));
    }

    @GetMapping("/{fileId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> getFileMetadata(
            @PathVariable Long fileId) {

        FileEntity file = fileService.getFileById(fileId);

        FileMetadataResponse response = FileMetadataResponse.builder()
                .fileId(file.getId())
                .fileName(file.getOriginalName())
                .fileType(file.getFileType())
                .size(file.getFileSize())
                .downloadUrl("/api/v1/files/" + file.getId() + "/download")
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "File ready for download", "/api/v1/files/" + fileId, response));
    }

    @GetMapping("/{fileId}/download")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        return fileService.downloadFile(fileId);
    }

    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<DeleteFileResponse>> deleteFile(@PathVariable Long fileId, HttpServletRequest httpServletRequest) {
        DeleteFileResponse deleteFileResponse =  fileService.deleteFile(fileId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "File deleted successfully.", httpServletRequest.getRequestURI()+ fileId, deleteFileResponse));
    }

    @PatchMapping("/{fileId}/rename")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<RenameFileResponse>> renameFile(@PathVariable Long fileId, @Valid @RequestBody RenameFileRequest request, HttpServletRequest httpRequest) {
        RenameFileResponse response = fileService.renameFile(fileId, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "File renamed successfully.", httpRequest.getRequestURI(), response));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PageResponse<FileSummaryResponse>>> getMyFiles(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "createdAt") String sortBy,

            @RequestParam(defaultValue = "desc") String direction,

            HttpServletRequest request) {

        PageResponse<FileSummaryResponse> response = fileService.getMyFiles(page, size, sortBy, direction);

        return  ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Files fetched successfully.", request.getRequestURI(), response));
    }

    @GetMapping("/trash")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PageResponse<FileSummaryResponse>>> getTrashFiles(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "deletedAt") String sortBy,

            @RequestParam(defaultValue = "desc") String direction,

            HttpServletRequest request) {

        PageResponse<FileSummaryResponse> response = fileService.getTrashFiles(page, size, sortBy, direction);

        return  ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "Trash files fetched successfully.", request.getRequestURI(), response));
    }
}
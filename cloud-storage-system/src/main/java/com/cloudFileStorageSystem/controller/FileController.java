package com.cloudFileStorageSystem.controller;

import com.cloudFileStorageSystem.dtos.request.FileUploadRequest;
import com.cloudFileStorageSystem.dtos.response.ApiResponse;
import com.cloudFileStorageSystem.dtos.response.FileUploadResponse;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.security.CustomUserDetailsService;
import com.cloudFileStorageSystem.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = Long.valueOf(userDetails.getUsername());

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
}
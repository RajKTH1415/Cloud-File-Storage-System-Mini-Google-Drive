package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


@Service
public class StorageServiceImpl implements StorageService {


    @Value("${file.storage.path}")
    private String rootDirectory;

    @Override
    public String store(MultipartFile file, Long userId) {

        try {

            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File is empty.");
            }

            String originalName = StringUtils.cleanPath(file.getOriginalFilename());

            String extension = getExtension(originalName);

            String category = getCategory(extension);

            Path uploadPath = Paths.get(
                    rootDirectory,
                    String.valueOf(userId),
                    category
            );

            Files.createDirectories(uploadPath);

            String storedFileName = UUID.randomUUID() + "." + extension;

            Path destination = uploadPath.resolve(storedFileName);

            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return destination.toString();

        } catch (IOException e) {
            throw new RuntimeException("Unable to store file.", e);
        }
    }

    @Override
    public void delete(String storagePath) {

    }

    @Override
    public byte[] load(String storagePath) {
        return new byte[0];
    }

    private String getExtension(String fileName) {

        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
    private String getCategory(String extension) {

        switch (extension) {

            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                return "images";

            case "pdf":
                return "pdf";

            case "mp4":
            case "avi":
            case "mov":
            case "mkv":
                return "videos";

            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
            case "txt":
                return "documents";

            default:
                return "others";
        }
    }
}

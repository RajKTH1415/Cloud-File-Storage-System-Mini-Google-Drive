package com.cloudFileStorageSystem.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String store(MultipartFile file, Long userId);

    void delete(String storagePath);

    byte[] load(String storagePath);
}

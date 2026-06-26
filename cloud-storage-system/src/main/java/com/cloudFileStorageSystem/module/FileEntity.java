package com.cloudFileStorageSystem.module;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
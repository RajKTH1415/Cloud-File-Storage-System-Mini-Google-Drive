package com.cloudFileStorageSystem.module;

import com.cloudFileStorageSystem.enums.FileCategory;
import com.cloudFileStorageSystem.enums.FileStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "files",
        indexes = {
                @Index(name = "idx_owner", columnList = "owner_id"),
                @Index(name = "idx_original_name", columnList = "original_name"),
                @Index(name = "idx_deleted", columnList = "is_deleted")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;


    @Column(name = "stored_name", nullable = false, unique = true, length = 255)
    private String storedName;


    @Column(name = "file_type", nullable = false, length = 150)
    private String fileType;


    @Column(name = "file_extension", nullable = false, length = 20)
    private String fileExtension;


    @Column(name = "file_size", nullable = false)
    private Long fileSize;


    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private FileCategory category;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FileStatus status;


    @Column(name = "folder_name", length = 255)
    private String folderName;


    @Column(name = "checksum", length = 64)
    private String checksum;


    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Users owner;
}
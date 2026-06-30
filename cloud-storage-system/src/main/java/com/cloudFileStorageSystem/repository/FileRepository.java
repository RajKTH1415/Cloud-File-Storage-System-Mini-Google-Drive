package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.module.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity,Long> {


    @Query("""
       SELECT f
       FROM FileEntity f
       JOIN FETCH f.owner
       WHERE f.id = :id
       AND f.isDeleted = false
       """)
    Optional<FileEntity> findByIdAndIsDeletedFalse(Long id);

    boolean existsByOwnerIdAndFolderNameAndOriginalNameAndIsDeletedFalse(
            Long ownerId,
            String folderName,
            String originalName
    );

    Page<FileEntity> findByOwnerIdAndIsDeletedFalse(
            Long ownerId,
            Pageable pageable
    );

    Page<FileEntity> findByOwnerIdAndIsDeletedTrue(
            Long ownerId,
            Pageable pageable
    );

    @Query("""
    SELECT f
    FROM FileEntity f
    WHERE f.owner.id = :ownerId
      AND f.isDeleted = false
      AND (
            LOWER(f.originalName) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(f.folderName) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(f.fileExtension) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    Page<FileEntity> searchFiles(
            @Param("ownerId") Long ownerId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}

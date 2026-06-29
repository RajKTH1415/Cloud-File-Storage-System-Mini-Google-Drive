package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.module.FileEntity;
import com.cloudFileStorageSystem.module.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}

package com.cloudFileStorageSystem.repository;

import com.cloudFileStorageSystem.module.FileEntity;
import com.cloudFileStorageSystem.module.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity,Long> {

    List<FileEntity> findByOwnerAndDeletedFalse(Users owner);

    Optional<FileEntity> findByIdAndDeletedFalse(Long id);

    Optional<FileEntity> findByStoredName(String storedName);

    boolean existsByStoredName(String storedName);
}

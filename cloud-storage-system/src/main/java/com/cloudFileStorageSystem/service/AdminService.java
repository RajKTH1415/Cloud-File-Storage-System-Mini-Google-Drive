package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.response.LockUserResponse;
import com.cloudFileStorageSystem.dtos.response.UnlockUserResponse;
import com.cloudFileStorageSystem.dtos.response.UsersResponse;
import com.cloudFileStorageSystem.enums.Role;

import java.util.List;

public interface AdminService {

    UnlockUserResponse unlockUser(Long userId);

    List<UsersResponse> getAllUsers();

    UsersResponse getUserById(Long userId);

    UsersResponse updateUserRole(Long userId, Role role);

    LockUserResponse lockUser(Long userId);

}
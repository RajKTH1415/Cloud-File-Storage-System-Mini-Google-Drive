package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.response.UnlockUserResponse;

public interface AdminService {

    UnlockUserResponse unlockUser(Long userId);

}
package com.cloudFileStorageSystem.service;

import com.cloudFileStorageSystem.dtos.request.UserRegistrationRequest;
import com.cloudFileStorageSystem.dtos.response.UserResponse;

public interface UserService {

    UserResponse registerUser(UserRegistrationRequest userRegistrationRequest);
}


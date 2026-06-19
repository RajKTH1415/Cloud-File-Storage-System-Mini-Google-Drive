package com.cloudFileStorageSystem.controller;

import com.cloudFileStorageSystem.dtos.request.UserRegistrationRequest;
import com.cloudFileStorageSystem.dtos.response.ApiResponse;
import com.cloudFileStorageSystem.dtos.response.UserResponse;
import com.cloudFileStorageSystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody UserRegistrationRequest registrationRequest, HttpServletRequest httpServletRequest){
        UserResponse userResponse = userService.registerUser(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED.value(), "User registered successfully",httpServletRequest.getRequestURI(), userResponse));
    }
}

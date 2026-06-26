package com.cloudFileStorageSystem.controller;

import com.cloudFileStorageSystem.dtos.request.UserRegistrationRequest;
import com.cloudFileStorageSystem.dtos.response.ApiResponse;
import com.cloudFileStorageSystem.dtos.response.UserResponse;
import com.cloudFileStorageSystem.service.Impl.UserServiceImpl;
import com.cloudFileStorageSystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(UsersController.class);


    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody UserRegistrationRequest request, HttpServletRequest servletRequest) {
        log.info("[REGISTER_USER] Request received | username={} | email={} | ip={} | uri={}", request.getUsername(), request.getEmail(), servletRequest.getRemoteAddr(), servletRequest.getRequestURI());
        UserResponse response = userService.registerUser(request);
        log.info("[REGISTER_USER] Registration completed | userId={} | username={}", response.getId(), response.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED.value(), "User registered successfully", servletRequest.getRequestURI(), response));
    }
}

package com.gulvisha.backend.controller;

import com.gulvisha.backend.dto.PasswordResetRequest;
import com.gulvisha.backend.dto.UserCreateRequest;
import com.gulvisha.backend.dto.UserResponse;
import com.gulvisha.backend.dto.UserUpdateRequest;
import com.gulvisha.backend.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Organization user management. Restricted to user:manage permission (admins only).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/password")
    public UserResponse resetPassword(@PathVariable UUID id, @RequestBody PasswordResetRequest request) {
        return userService.resetPassword(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return userService.updateStatus(id, body.getOrDefault("status", ""));
    }
}

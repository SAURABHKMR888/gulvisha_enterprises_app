package com.gulvisha.backend.dto;

import com.gulvisha.backend.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String role,
        String status,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole().name(), user.getStatus(), user.getCreatedAt());
    }
}

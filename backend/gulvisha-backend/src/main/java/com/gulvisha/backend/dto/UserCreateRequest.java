package com.gulvisha.backend.dto;

import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 100) String fullName,
        @NotBlank String role
) {
}

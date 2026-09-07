package com.gulvisha.backend.dto;

import jakarta.validation.constraints.*;

public record UserUpdateRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 100) String fullName,
        @NotBlank String role
) {
}

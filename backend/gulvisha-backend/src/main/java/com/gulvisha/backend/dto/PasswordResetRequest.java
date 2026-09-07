package com.gulvisha.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}

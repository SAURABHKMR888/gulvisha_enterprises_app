package com.gulvisha.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuoteRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email must be 254 characters or fewer")
        String email,

        @Size(max = 120, message = "Company must be 120 characters or fewer")
        String company,

        @NotBlank(message = "Please select a service")
        @Size(max = 80, message = "Service must be 80 characters or fewer")
        String service,

        @NotBlank(message = "Please tell us a little about your project")
        @Size(max = 2_000, message = "Project details must be 2,000 characters or fewer")
        String details
) {
}
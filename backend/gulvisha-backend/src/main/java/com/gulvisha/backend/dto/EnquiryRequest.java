package com.gulvisha.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnquiryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 120) String companyName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 80) String serviceCategory,
        @NotBlank @Size(max = 80) String service,
        @NotBlank @Size(max = 2_000) String message,
        @Size(max = 80) String budget,
        @Size(max = 80) String timeline,
        @Size(max = 20) String source,
        @Size(max = 30) String preferredContactMethod
) {
}
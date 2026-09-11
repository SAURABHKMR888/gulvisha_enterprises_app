package com.gulvisha.backend.crm;

import jakarta.validation.constraints.*;

/**
 * Request to provision a portal user (CLIENT role) linked to an existing client record.
 * The created user can sign in and access only that client's data via the client portal.
 */
public record ClientPortalUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 100) String fullName
) {}

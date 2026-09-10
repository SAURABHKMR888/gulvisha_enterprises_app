package com.gulvisha.backend.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TenantRequests {

    public record TenantCreateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String displayName,
            @Size(max = 100) String slug,
            @Size(max = 500) String description,
            @Size(max = 100) String industry,
            @Email @Size(max = 254) String email,
            @Size(max = 30) String phone,
            @Size(max = 300) String website,
            @Size(max = 500) String address,
            @Size(max = 7) String primaryColor,
            @Size(max = 7) String accentColor,
            @Size(max = 500) String logoUrl,
            @Size(max = 500) String faviconUrl,
            @Size(max = 100) String timezone,
            @Size(max = 10) String currency,
            @Size(max = 10) String language,
            String siteContent,
            String adminUsername,
            String adminPassword,
            String adminName,
            String adminEmail
    ) {}

    public record TenantUpdateRequest(
            @Size(max = 200) String name,
            @Size(max = 200) String displayName,
            @Size(max = 100) String slug,
            @Size(max = 500) String description,
            @Size(max = 100) String industry,
            @Email @Size(max = 254) String email,
            @Size(max = 30) String phone,
            @Size(max = 300) String website,
            @Size(max = 500) String address,
            @Size(max = 7) String primaryColor,
            @Size(max = 7) String accentColor,
            @Size(max = 500) String logoUrl,
            @Size(max = 500) String faviconUrl,
            @Size(max = 100) String timezone,
            @Size(max = 10) String currency,
            @Size(max = 10) String language,
            String siteContent
    ) {}
}

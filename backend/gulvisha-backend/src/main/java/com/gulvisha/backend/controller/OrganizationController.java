package com.gulvisha.backend.controller;

import com.gulvisha.backend.organization.Organization;
import com.gulvisha.backend.organization.OrganizationRepository;
import com.gulvisha.backend.security.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Organization settings API — lets each tenant configure its own
 * company profile (branding, contact info, locale preferences).
 * Protected by PERMISSION_organization:settings (admins only).
 */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationRepository organizationRepository;

    public OrganizationController(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @GetMapping("/current")
    public Organization current() {
        return organizationRepository.findById(resolveOrganizationId())
                .orElseThrow(() -> new IllegalStateException("No organization configured"));
    }

    @PutMapping("/current")
    public Organization update(@Valid @RequestBody OrganizationSettingsRequest request) {
        Organization organization = organizationRepository.findById(resolveOrganizationId())
                .orElseThrow(() -> new IllegalStateException("No organization configured"));
        organization.setName(request.name());
        organization.setDescription(request.description());
        organization.setIndustry(request.industry());
        organization.setEmail(request.email());
        organization.setPhone(request.phone());
        organization.setWebsite(request.website());
        organization.setAddress(request.address());
        organization.setTimezone(request.timezone());
        organization.setCurrency(request.currency());
        organization.setLanguage(request.language());
        return organizationRepository.save(organization);
    }

    /** Prefer the authenticated user's organization; fall back to the seeded default (single-tenant dev). */
    private UUID resolveOrganizationId() {
        UUID contextOrgId = UserContext.getOrganizationId();
        if (contextOrgId != null && organizationRepository.existsById(contextOrgId)) {
            return contextOrgId;
        }
        return organizationRepository.findAll().stream()
                .findFirst()
                .map(Organization::getId)
                .orElseThrow(() -> new IllegalStateException("No organization configured"));
    }

    public record OrganizationSettingsRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String description,
            @Size(max = 100) String industry,
            @Email @Size(max = 254) String email,
            @Size(max = 30) String phone,
            @Size(max = 300) String website,
            @Size(max = 500) String address,
            @Size(max = 100) String timezone,
            @Size(max = 10) String currency,
            @Size(max = 10) String language
    ) {
    }
}

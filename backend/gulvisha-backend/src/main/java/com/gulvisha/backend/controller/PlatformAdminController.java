package com.gulvisha.backend.controller;

import com.gulvisha.backend.organization.Organization;
import com.gulvisha.backend.organization.OrganizationRepository;
import com.gulvisha.backend.user.User;
import com.gulvisha.backend.user.UserRepository;
import com.gulvisha.backend.security.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/organizations")
public class PlatformAdminController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PlatformAdminController(OrganizationRepository organizationRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Organization> list() {
        return organizationRepository.findAllByOrderByNameAsc();
    }

    @GetMapping("/{id}")
    public Organization get(@PathVariable UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
    }

    @PostMapping
    public ResponseEntity<Organization> create(@Valid @RequestBody TenantRequests.TenantCreateRequest request) {
        Organization org = new Organization(request.name());
        org.setDisplayName(request.displayName());
        org.setSlug(request.slug());
        org.setDescription(request.description());
        org.setIndustry(request.industry());
        org.setEmail(request.email());
        org.setPhone(request.phone());
        org.setWebsite(request.website());
        org.setAddress(request.address());
        org.setPrimaryColor(request.primaryColor());
        org.setAccentColor(request.accentColor());
        org.setLogoUrl(request.logoUrl());
        org.setFaviconUrl(request.faviconUrl());
        org.setTimezone(request.timezone());
        org.setCurrency(request.currency());
        org.setLanguage(request.language());
        org.setSiteContent(request.siteContent());
        org.setStatus(Organization.STATUS_ONBOARDING);
        Organization savedOrg = organizationRepository.save(org);

        if (request.adminUsername() != null && request.adminPassword() != null) {
            User admin = new User(
                    savedOrg.getId(),
                    request.adminUsername(),
                    request.adminEmail() != null ? request.adminEmail() : request.email(),
                    passwordEncoder.encode(request.adminPassword()),
                    request.adminName() != null ? request.adminName() : request.displayName(),
                    Role.ORGANIZATION_ADMIN
            );
            userRepository.save(admin);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrg);
    }

    @PutMapping("/{id}")
    public Organization update(@PathVariable UUID id, @Valid @RequestBody TenantRequests.TenantUpdateRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        if (request.name() != null) org.setName(request.name());
        if (request.displayName() != null) org.setDisplayName(request.displayName());
        if (request.slug() != null) org.setSlug(request.slug());
        if (request.description() != null) org.setDescription(request.description());
        if (request.industry() != null) org.setIndustry(request.industry());
        if (request.email() != null) org.setEmail(request.email());
        if (request.phone() != null) org.setPhone(request.phone());
        if (request.website() != null) org.setWebsite(request.website());
        if (request.address() != null) org.setAddress(request.address());
        if (request.primaryColor() != null) org.setPrimaryColor(request.primaryColor());
        if (request.accentColor() != null) org.setAccentColor(request.accentColor());
        if (request.logoUrl() != null) org.setLogoUrl(request.logoUrl());
        if (request.faviconUrl() != null) org.setFaviconUrl(request.faviconUrl());
        if (request.timezone() != null) org.setTimezone(request.timezone());
        if (request.currency() != null) org.setCurrency(request.currency());
        if (request.language() != null) org.setLanguage(request.language());
        if (request.siteContent() != null) org.setSiteContent(request.siteContent());
        return organizationRepository.save(org);
    }

    @PatchMapping("/{id}/activate")
    public Organization activate(@PathVariable UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        org.setStatus(Organization.STATUS_ACTIVE);
        return organizationRepository.save(org);
    }

    @PatchMapping("/{id}/suspend")
    public Organization suspend(@PathVariable UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        org.setStatus(Organization.STATUS_SUSPENDED);
        return organizationRepository.save(org);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        organizationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

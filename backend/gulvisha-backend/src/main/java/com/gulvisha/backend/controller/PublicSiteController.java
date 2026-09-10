package com.gulvisha.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulvisha.backend.organization.Organization;
import com.gulvisha.backend.organization.OrganizationRepository;
import com.gulvisha.backend.organization.SiteContent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * Public endpoint that returns the active tenant's branding/configuration
 * for the public website. Resolves tenant by slug (from subdomain) or
 * falls back to the first organization (single-tenant dev).
 */
@RestController
@RequestMapping("/api/public")
public class PublicSiteController {

    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    public PublicSiteController(OrganizationRepository organizationRepository, ObjectMapper objectMapper) {
        this.organizationRepository = organizationRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/site")
    public SiteConfigResponse site(@RequestParam Optional<String> slug) {
        Organization org;
        if (slug.isPresent() && !slug.get().isBlank()) {
            org = organizationRepository.findBySlug(slug.get())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown tenant: " + slug.get()));
        } else {
            org = organizationRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No organization configured"));
        }
        return new SiteConfigResponse(
                org.getId(),
                org.getName(),
                org.getDisplayName(),
                org.getSlug(),
                org.getDescription(),
                org.getLogoUrl(),
                org.getFaviconUrl(),
                org.getPrimaryColor(),
                org.getAccentColor(),
                org.getIndustry(),
                org.getEmail(),
                org.getPhone(),
                org.getWebsite(),
                org.getAddress(),
                parseSiteContent(org.getSiteContent())
        );
    }

    private JsonNode parseSiteContent(String siteContent) {
        if (siteContent == null || siteContent.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(siteContent);
        } catch (Exception e) {
            return null;
        }
    }

    public record SiteConfigResponse(
            UUID organizationId,
            String name,
            String displayName,
            String slug,
            String description,
            String logoUrl,
            String faviconUrl,
            String primaryColor,
            String accentColor,
            String industry,
            String email,
            String phone,
            String website,
            String address,
            JsonNode siteContent
    ) {
    }
}
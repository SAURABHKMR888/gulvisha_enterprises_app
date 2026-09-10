package com.gulvisha.backend.controller;

import com.gulvisha.backend.organization.Organization;
import com.gulvisha.backend.organization.OrganizationRepository;
import com.gulvisha.backend.security.UserContext;
import com.gulvisha.backend.service.Service;
import com.gulvisha.backend.service.ServiceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final OrganizationRepository organizationRepository;

    public ServiceController(ServiceRepository serviceRepository,
                             OrganizationRepository organizationRepository) {
        this.serviceRepository = serviceRepository;
        this.organizationRepository = organizationRepository;
    }

    /** Public endpoint: resolves tenant by slug param, falling back to first org for the public website. */
    @GetMapping
    public List<Service> list(@RequestParam(required = false) String slug) {
        UUID orgId = resolveOrganizationId(slug);
        return serviceRepository.findAllByOrganizationIdAndPublicVisibleTrueOrderByDisplayOrderAsc(orgId);
    }

    /** Creates a service for the authenticated user's organization. */
    @PostMapping
    public ResponseEntity<Service> create(@Valid @RequestBody ServiceRequest request) {
        UUID orgId = UserContext.getOrganizationId();
        Service service = new Service(orgId, request.name());
        service.setDescription(request.description());
        service.setCategory(request.category());
        service.setPricingInfo(request.pricingInfo());
        service.setDisplayOrder(request.displayOrder());
        service.setPublicVisible(request.publicVisible());
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceRepository.save(service));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Service> update(@PathVariable UUID id, @Valid @RequestBody ServiceRequest request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
        if (!service.getOrganizationId().equals(UserContext.getOrganizationId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.setName(request.name());
        service.setDescription(request.description());
        service.setCategory(request.category());
        service.setPricingInfo(request.pricingInfo());
        service.setDisplayOrder(request.displayOrder());
        service.setPublicVisible(request.publicVisible());
        return ResponseEntity.ok(serviceRepository.save(service));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
        if (!service.getOrganizationId().equals(UserContext.getOrganizationId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        serviceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveOrganizationId(String slug) {
        if (slug != null && !slug.isBlank()) {
            return organizationRepository.findBySlug(slug)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown tenant: " + slug))
                    .getId();
        }
        // Fallback for the public website (single-tenant mode): first org
        return organizationRepository.findAll().stream()
                .findFirst()
                .map(Organization::getId)
                .orElseThrow(() -> new IllegalStateException("No organization configured"));
    }

    public record ServiceRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @Size(max = 100) String category,
            @Size(max = 100) String pricingInfo,
            Integer displayOrder,
            Boolean publicVisible
    ) {
    }
}

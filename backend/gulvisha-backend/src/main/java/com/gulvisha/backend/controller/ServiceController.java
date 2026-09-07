package com.gulvisha.backend.controller;

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
    private final com.gulvisha.backend.organization.OrganizationRepository organizationRepository;

    public ServiceController(ServiceRepository serviceRepository,
                             com.gulvisha.backend.organization.OrganizationRepository organizationRepository) {
        this.serviceRepository = serviceRepository;
        this.organizationRepository = organizationRepository;
    }

    @GetMapping
    public List<Service> list() {
        UUID orgId = getDefaultOrganizationId();
        return serviceRepository.findAllByOrganizationIdOrderByNameAsc(orgId);
    }

    @PostMapping
    public ResponseEntity<Service> create(@Valid @RequestBody ServiceRequest request) {
        UUID orgId = getDefaultOrganizationId();
        Service service = new Service(orgId, request.name());
        service.setDescription(request.description());
        service.setCategory(request.category());
        service.setPricingInfo(request.pricingInfo());
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceRepository.save(service));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Service> update(@PathVariable UUID id, @Valid @RequestBody ServiceRequest request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
        service.setName(request.name());
        service.setDescription(request.description());
        service.setCategory(request.category());
        service.setPricingInfo(request.pricingInfo());
        return ResponseEntity.ok(serviceRepository.save(service));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UUID getDefaultOrganizationId() {
        return organizationRepository.findAll().stream()
                .findFirst()
                .map(com.gulvisha.backend.organization.Organization::getId)
                .orElseThrow(() -> new IllegalStateException("No organization configured"));
    }

    public record ServiceRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @Size(max = 100) String category,
            @Size(max = 100) String pricingInfo
    ) {
    }
}

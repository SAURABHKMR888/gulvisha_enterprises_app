package com.gulvisha.backend.controller;

import com.gulvisha.backend.resource.Resource;
import com.gulvisha.backend.resource.ResourceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public Page<Resource> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {
        return resourceService.getResources(PageRequest.of(page, size), status, role, search);
    }

    @GetMapping("/{id}")
    public Resource getById(@PathVariable UUID id) {
        return resourceService.getResourceById(id);
    }

    @PostMapping
    public ResponseEntity<Resource> create(@RequestBody ResourceService.ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createResource(request));
    }

    @PutMapping("/{id}")
    public Resource update(@PathVariable UUID id, @RequestBody ResourceService.ResourceRequest request) {
        return resourceService.updateResource(id, request);
    }

    @PatchMapping("/{id}/status")
    public Resource updateStatus(@PathVariable UUID id, @RequestBody StatusUpdateRequest request) {
        return resourceService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }

    public record StatusUpdateRequest(String status) {}
}

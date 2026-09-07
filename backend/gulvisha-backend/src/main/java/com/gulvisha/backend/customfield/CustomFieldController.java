package com.gulvisha.backend.customfield;

import com.gulvisha.backend.organization.OrganizationRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/custom-fields")
public class CustomFieldController {
    private final CustomFieldDefinitionRepository repository;
    private final OrganizationRepository organizationRepository;
    
    public CustomFieldController(CustomFieldDefinitionRepository repository, OrganizationRepository organizationRepository) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
    }
    
    private UUID getDefaultOrgId() {
        return organizationRepository.findAll().stream()
            .findFirst()
            .map(com.gulvisha.backend.organization.Organization::getId)
            .orElseThrow(() -> new IllegalStateException("No organization configured"));
    }
    
    @GetMapping
    public List<CustomFieldDefinition> list(@RequestParam String entityType) {
        return repository.findAllByOrganizationIdAndEntityTypeOrderByCreatedAt(getDefaultOrgId(), entityType);
    }
    
    @PostMapping
    public ResponseEntity<CustomFieldDefinition> create(@Valid @RequestBody CustomFieldRequest request) {
        CustomFieldDefinition field = new CustomFieldDefinition(
            getDefaultOrgId(), request.name(), request.entityType(), 
            request.fieldType(), request.options(), request.required()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(field));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CustomFieldDefinition> update(@PathVariable UUID id, @Valid @RequestBody CustomFieldRequest request) {
        CustomFieldDefinition field = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Field not found: " + id));
        field.setName(request.name());
        field.setFieldType(request.fieldType());
        field.setOptions(request.options());
        field.setRequired(request.required());
        return ResponseEntity.ok(repository.save(field));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    public record CustomFieldRequest(
        String name,
        String entityType,
        String fieldType,
        String options,
        boolean required
    ) {}
}
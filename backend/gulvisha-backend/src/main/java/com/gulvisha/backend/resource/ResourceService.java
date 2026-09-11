package com.gulvisha.backend.resource;

import com.gulvisha.backend.project.Project;
import com.gulvisha.backend.project.ProjectRepository;
import com.gulvisha.backend.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final ProjectRepository projectRepository;

    public ResourceService(ResourceRepository resourceRepository,
                           ProjectRepository projectRepository) {
        this.resourceRepository = resourceRepository;
        this.projectRepository = projectRepository;
    }

    private UUID getOrgId() {
        UUID orgId = UserContext.getOrganizationId();
        if (orgId == null) {
            throw new IllegalStateException("No organization context");
        }
        return orgId;
    }

    public Page<Resource> getResources(Pageable pageable, String status, String role, String search) {
        UUID orgId = getOrgId();
        Specification<Resource> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("organizationId"), orgId));

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (role != null && !role.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("role")), role.toLowerCase()));
        }
        if (search != null && !search.isBlank()) {
            String term = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), term),
                    cb.like(cb.lower(root.get("skills")), term)
            ));
        }

        return resourceRepository.findAll(spec, pageable);
    }

    public Resource getResourceById(UUID id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + id));
        if (!resource.getOrganizationId().equals(getOrgId())) {
            // Tenant isolation: never reveal that a resource exists in another organization
            throw new IllegalArgumentException("Resource not found: " + id);
        }
        return resource;
    }

    public Resource createResource(ResourceRequest request) {
        UUID orgId = getOrgId();
        validateProject(orgId, request.assignedProjectId());

        Resource resource = new Resource(orgId, request.name(), request.role());
        applyOptionalFields(resource, request);
        return resourceRepository.save(resource);
    }

    public Resource updateResource(UUID id, ResourceRequest request) {
        Resource resource = getResourceById(id);
        validateProject(resource.getOrganizationId(), request.assignedProjectId());

        resource.setName(request.name());
        resource.setRole(request.role());
        applyOptionalFields(resource, request);
        return resourceRepository.save(resource);
    }

    public Resource updateStatus(UUID id, String status) {
        Resource resource = getResourceById(id);
        resource.setStatus(status);
        return resourceRepository.save(resource);
    }

    public void deleteResource(UUID id) {
        Resource resource = getResourceById(id);
        resourceRepository.delete(resource);
    }

    private void validateProject(UUID orgId, UUID projectId) {
        if (projectId == null) {
            return;
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        if (!project.getOrganizationId().equals(orgId)) {
            // Tenant isolation: project belongs to a different organization
            throw new IllegalArgumentException("Project not found: " + projectId);
        }
    }

    private void applyOptionalFields(Resource resource, ResourceRequest request) {
        resource.setSkills(request.skills());
        resource.setExperienceYears(request.experienceYears());
        resource.setAvailability(request.availability());
        if (request.status() != null && !request.status().isBlank()) {
            resource.setStatus(request.status());
        }
        resource.setAssignedProjectId(request.assignedProjectId());
    }

    public record ResourceRequest(
            String name,
            String role,
            String skills,
            Integer experienceYears,
            String availability,
            String status,
            UUID assignedProjectId
    ) {}
}

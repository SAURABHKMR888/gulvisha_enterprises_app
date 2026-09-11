package com.gulvisha.backend.project;

import com.gulvisha.backend.crm.ClientRepository;
import com.gulvisha.backend.security.UserContext;
import com.gulvisha.backend.workflow.WorkflowEngine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final WorkflowEngine workflowEngine;

    public ProjectService(ProjectRepository projectRepository,
                          ClientRepository clientRepository,
                          WorkflowEngine workflowEngine) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.workflowEngine = workflowEngine;
    }

    private UUID getOrgId() {
        UUID orgId = UserContext.getOrganizationId();
        if (orgId == null) {
            throw new IllegalStateException("No organization context");
        }
        return orgId;
    }

    public Page<Project> getProjects(Pageable pageable, String status, String search) {
        UUID orgId = getOrgId();
        Specification<Project> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("organizationId"), orgId));

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (search != null && !search.isBlank()) {
            String term = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), term),
                    cb.like(cb.lower(root.get("service")), term)
            ));
        }

        return projectRepository.findAll(spec, pageable);
    }

    public Project getProjectById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        if (!project.getOrganizationId().equals(getOrgId())) {
            // Tenant isolation: never reveal that a project exists in another organization
            throw new IllegalArgumentException("Project not found: " + id);
        }
        return project;
    }

    public Project createProject(ProjectRequest request) {
        UUID orgId = getOrgId();
        validateClient(orgId, request.clientId());

        Project project = new Project(
                orgId,
                request.clientId(),
                request.name(),
                request.description(),
                request.service()
        );
        applyOptionalFields(project, request);
        if (request.status() != null && !request.status().isBlank()) {
            project.setStatus(request.status());
        }
        Project saved = projectRepository.save(project);
        // Orchestration: fire PROJECT_CREATED so tenant-configured workflows run (e.g. Create task, Update status).
        workflowEngine.executeForTrigger(orgId, "PROJECT_CREATED", saved.getId());
        return saved;
    }

    public Project updateProject(UUID id, ProjectRequest request) {
        Project project = getProjectById(id);
        validateClient(project.getOrganizationId(), request.clientId());

        project.setClientId(request.clientId());
        project.setName(request.name());
        project.setDescription(request.description());
        project.setService(request.service());
        applyOptionalFields(project, request);
        if (request.status() != null && !request.status().isBlank()) {
            project.setStatus(request.status());
        }
        return projectRepository.save(project);
    }

    public Project updateStatus(UUID id, String status) {
        Project project = getProjectById(id);
        project.setStatus(status);
        return projectRepository.save(project);
    }

    public void deleteProject(UUID id) {
        Project project = getProjectById(id);
        projectRepository.delete(project);
    }

    private void validateClient(UUID orgId, UUID clientId) {
        if (clientId == null) {
            return;
        }
        clientRepository.findById(clientId)
                .filter(client -> client.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Client not found in this organization: " + clientId));
    }

    private void applyOptionalFields(Project project, ProjectRequest request) {
        project.setStartDate(request.startDate() != null && !request.startDate().isBlank()
                ? LocalDate.parse(request.startDate()) : null);
        project.setEndDate(request.endDate() != null && !request.endDate().isBlank()
                ? LocalDate.parse(request.endDate()) : null);
        project.setBudget(request.budget() != null && !request.budget().isBlank()
                ? new BigDecimal(request.budget()) : null);
        project.setManager(request.manager());
    }

    public record ProjectRequest(
            String name,
            String description,
            String service,
            UUID clientId,
            String status,
            String startDate,
            String endDate,
            String budget,
            String manager
    ) {}
}

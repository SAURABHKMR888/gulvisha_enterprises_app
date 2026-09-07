package com.gulvisha.backend.task;

import com.gulvisha.backend.organization.Organization;
import com.gulvisha.backend.organization.OrganizationRepository;
import com.gulvisha.backend.project.Project;
import com.gulvisha.backend.project.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       OrganizationRepository organizationRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
    }

    private UUID getDefaultOrgId() {
        return organizationRepository.findAll().stream()
                .findFirst()
                .map(Organization::getId)
                .orElseThrow(() -> new IllegalStateException("No organization configured"));
    }

    public Page<Task> getTasks(Pageable pageable, UUID projectId, String status, String assignedTo) {
        UUID orgId = getDefaultOrgId();
        Specification<Task> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("organizationId"), orgId));

        if (projectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("projectId"), projectId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (assignedTo != null && !assignedTo.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("assignedTo")), assignedTo.toLowerCase()));
        }

        return taskRepository.findAll(spec, pageable);
    }

    public Task getTaskById(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        if (!task.getOrganizationId().equals(getDefaultOrgId())) {
            // Tenant isolation: never reveal that a task exists in another organization
            throw new IllegalArgumentException("Task not found: " + id);
        }
        return task;
    }

    public Task createTask(TaskRequest request) {
        UUID orgId = getDefaultOrgId();
        validateProject(orgId, request.projectId());

        Task task = new Task(orgId, request.projectId(), request.title(), request.description());
        applyOptionalFields(task, request);
        return taskRepository.save(task);
    }

    public Task updateTask(UUID id, TaskRequest request) {
        Task task = getTaskById(id);
        validateProject(task.getOrganizationId(), request.projectId());

        task.setProjectId(request.projectId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        applyOptionalFields(task, request);
        return taskRepository.save(task);
    }

    public Task updateStatus(UUID id, String status) {
        Task task = getTaskById(id);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    public void deleteTask(UUID id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
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

    private void applyOptionalFields(Task task, TaskRequest request) {
        task.setAssignedTo(request.assignedTo());
        if (request.priority() != null && !request.priority().isBlank()) {
            task.setPriority(request.priority());
        }
        if (request.status() != null && !request.status().isBlank()) {
            task.setStatus(request.status());
        }
        task.setDueDate(request.dueDate() != null && !request.dueDate().isBlank()
                ? LocalDate.parse(request.dueDate()) : null);
    }

    public record TaskRequest(
            String title,
            String description,
            UUID projectId,
            String assignedTo,
            String priority,
            String status,
            String dueDate
    ) {}
}

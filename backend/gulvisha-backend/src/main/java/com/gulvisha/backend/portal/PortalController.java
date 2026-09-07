package com.gulvisha.backend.portal;

import com.gulvisha.backend.crm.Client;
import com.gulvisha.backend.project.Project;
import com.gulvisha.backend.task.Task;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Client Portal API. Accessible only to users with the CLIENT role
 * (SecurityConfig requires PERMISSION_project:view, which only CLIENT has
 * among portal-relevant roles, and PortalService additionally enforces a
 * non-null clientId from the JWT).
 */
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    public record TaskView(UUID id, UUID projectId, String title, String description,
                           String assignedTo, String status, String priority, String dueDate) {}

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/profile")
    public Client profile() {
        return portalService.getProfile();
    }

    @GetMapping("/dashboard")
    public PortalService.Dashboard dashboard() {
        return portalService.getDashboard();
    }

    @GetMapping("/projects")
    public List<Project> projects() {
        return portalService.getProjects();
    }

    @GetMapping("/tasks")
    public List<TaskView> tasks() {
        return portalService.getTasks().stream().map(PortalController::toView).toList();
    }

    private static TaskView toView(Task task) {
        return new TaskView(
                task.getId(),
                task.getProjectId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssignedTo(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate() == null ? null : task.getDueDate().toString());
    }
}
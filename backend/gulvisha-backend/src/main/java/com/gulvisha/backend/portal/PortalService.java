package com.gulvisha.backend.portal;

import com.gulvisha.backend.crm.Client;
import com.gulvisha.backend.crm.ClientRepository;
import com.gulvisha.backend.project.Project;
import com.gulvisha.backend.project.ProjectRepository;
import com.gulvisha.backend.security.UserContext;
import com.gulvisha.backend.task.Task;
import com.gulvisha.backend.task.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-only data access for the client portal.
 * Every query is restricted to the client linked to the authenticated user
 * (clientId from the JWT via UserContext). This is the tenant-isolation boundary
 * for portal users: a CLIENT user can never read another client's data.
 */
@Service
public class PortalService {

    public record Dashboard(long totalProjects, long activeProjects,
                            long openTasks, long inProgressTasks, long completedTasks) {}

    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public PortalService(ClientRepository clientRepository,
                         ProjectRepository projectRepository,
                         TaskRepository taskRepository) {
        this.clientRepository = clientRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    public Client getProfile() {
        return clientRepository.findByIdAndOrganizationId(requireClientId(), requireOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client profile not found"));
    }

    public List<Project> getProjects() {
        return projectRepository.findAllByOrganizationIdAndClientId(requireOrganizationId(), requireClientId());
    }

    public List<Task> getTasks() {
        List<UUID> projectIds = getProjects().stream().map(Project::getId).toList();
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return taskRepository.findAllByOrganizationIdAndProjectIdInOrderByCreatedAtDesc(
                requireOrganizationId(), projectIds);
    }

    public Dashboard getDashboard() {
        UUID organizationId = requireOrganizationId();
        UUID clientId = requireClientId();

        List<Project> projects = projectRepository.findAllByOrganizationIdAndClientId(organizationId, clientId);
        long activeProjects = projects.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count();

        long openTasks = 0;
        long inProgressTasks = 0;
        long completedTasks = 0;
        List<UUID> projectIds = projects.stream().map(Project::getId).toList();
        if (!projectIds.isEmpty()) {
            List<Task> tasks = taskRepository.findAllByOrganizationIdAndProjectIdInOrderByCreatedAtDesc(
                    organizationId, projectIds);
            openTasks = tasks.stream().filter(t -> "TO_DO".equals(t.getStatus())).count();
            inProgressTasks = tasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
            completedTasks = tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        }

        return new Dashboard(projects.size(), activeProjects, openTasks, inProgressTasks, completedTasks);
    }

    private UUID requireClientId() {
        UUID clientId = UserContext.getClientId();
        if (clientId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This account does not have client portal access");
        }
        return clientId;
    }

    private UUID requireOrganizationId() {
        UUID organizationId = UserContext.getOrganizationId();
        if (organizationId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return organizationId;
    }
}
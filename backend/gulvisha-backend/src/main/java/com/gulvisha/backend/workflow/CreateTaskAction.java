package com.gulvisha.backend.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulvisha.backend.project.Project;
import com.gulvisha.backend.project.ProjectRepository;
import com.gulvisha.backend.task.Task;
import com.gulvisha.backend.task.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creates a task for the entity the workflow fired for.
 * If the entity is a project (PROJECT_CREATED trigger), the task is linked to it;
 * otherwise a standalone organisational task is created.
 *
 * Config (JSON): {"title":"...", "description":"...", "priority":"HIGH|MEDIUM|LOW",
 *                  "assignedTo":"...", "status":"..."}
 */
@Component
public class CreateTaskAction implements WorkflowAction {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateTaskAction(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public String type() {
        return "CREATE_TASK";
    }

    @Override
    public String execute(UUID organizationId, UUID entityId, String config) {
        Map<String, Object> cfg = parseConfig(config);
        String title = cfg.get("title") == null ? null : cfg.get("title").toString().trim();
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("CREATE_TASK requires config: {\"title\": \"...\"}");
        }

        UUID projectId = null;
        String linkedTo = "organization";
        if (entityId != null) {
            Project project = projectRepository.findById(entityId)
                    .filter(p -> p.getOrganizationId().equals(organizationId))
                    .orElse(null);
            if (project != null) {
                projectId = project.getId();
                linkedTo = "project '" + project.getName() + "'";
            }
        }

        Task task = new Task(organizationId, projectId, title,
                cfg.get("description") == null ? null : cfg.get("description").toString());
        if (cfg.get("priority") != null && !cfg.get("priority").toString().isBlank()) {
            task.setPriority(cfg.get("priority").toString().trim().toUpperCase());
        }
        if (cfg.get("assignedTo") != null && !cfg.get("assignedTo").toString().isBlank()) {
            task.setAssignedTo(cfg.get("assignedTo").toString().trim());
        }
        if (cfg.get("status") != null && !cfg.get("status").toString().isBlank()) {
            task.setStatus(cfg.get("status").toString().trim());
        }
        Task saved = taskRepository.save(task);
        return "Created task '" + saved.getTitle() + "' (id=" + saved.getId() + ") for " + linkedTo;
    }

    private Map<String, Object> parseConfig(String config) {
        if (config == null || config.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(config,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid config JSON: " + config);
        }
    }
}
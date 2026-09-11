package com.gulvisha.backend.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulvisha.backend.crm.Lead;
import com.gulvisha.backend.crm.LeadRepository;
import com.gulvisha.backend.project.Project;
import com.gulvisha.backend.project.ProjectRepository;
import com.gulvisha.backend.task.Task;
import com.gulvisha.backend.task.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Updates the status/stage of the entity the workflow fired for.
 *
 * Config (JSON): {"entity":"PROJECT|LEAD|TASK", "status":"..."}  (entity defaults to PROJECT)
 * For LEADS the status value is the pipeline stage name (e.g. "Qualified").
 */
@Component
public class UpdateStatusAction implements WorkflowAction {

    private final ProjectRepository projectRepository;
    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UpdateStatusAction(ProjectRepository projectRepository,
                              LeadRepository leadRepository,
                              TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.leadRepository = leadRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public String type() {
        return "UPDATE_STATUS";
    }

    @Override
    public String execute(UUID organizationId, UUID entityId, String config) {
        Map<String, Object> cfg = parseConfig(config);
        String entity = cfg.get("entity") == null ? "PROJECT" : cfg.get("entity").toString().trim().toUpperCase();
        Object statusVal = cfg.get("status");
        String status = statusVal == null ? null : statusVal.toString().trim();
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("UPDATE_STATUS requires config: {\"status\": \"...\"}");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("UPDATE_STATUS requires an entityId from the trigger");
        }

        switch (entity) {
            case "PROJECT" -> {
                Project project = projectRepository.findById(entityId)
                        .filter(p -> p.getOrganizationId().equals(organizationId))
                        .orElseThrow(() -> new IllegalArgumentException("Project not found: " + entityId));
                project.setStatus(status);
                projectRepository.save(project);
                return "Updated project '" + project.getName() + "' status to " + status;
            }
            case "LEAD" -> {
                Lead lead = leadRepository.findById(entityId)
                        .filter(l -> l.getOrganizationId().equals(organizationId))
                        .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + entityId));
                lead.setStatus(status);
                leadRepository.save(lead);
                return "Updated lead '" + lead.getFirstName() + "' stage to " + status;
            }
            case "TASK" -> {
                Task task = taskRepository.findById(entityId)
                        .filter(t -> t.getOrganizationId().equals(organizationId))
                        .orElseThrow(() -> new IllegalArgumentException("Task not found: " + entityId));
                task.setStatus(status);
                taskRepository.save(task);
                return "Updated task '" + task.getTitle() + "' status to " + status;
            }
            default -> throw new IllegalArgumentException(
                    "Unknown entity type: " + entity + " (expected PROJECT, LEAD or TASK)");
        }
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
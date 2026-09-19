package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.task.Task;
import com.gulvisha.backend.task.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Updates a task in the agent's own organization.
 * High-impact: requires human approval via the guardrail layer.
 * Args: {id: task UUID, status?: string, assignedTo?: string, priority?: LOW|MEDIUM|HIGH}.
 */
@Component
public class UpdateTaskTool implements AgentTool {

    private final TaskRepository taskRepository;

    public UpdateTaskTool(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public String name() { return "update_task"; }

    @Override
    public String description() {
        return "Update a task's status, assignee or priority in this organization.";
    }

    @Override
    public String argumentSpec() {
        return "{ \"id\": \"required task UUID\", \"status\": \"optional\", "
                + "\"assignedTo\": \"optional username\", \"priority\": \"optional LOW|MEDIUM|HIGH\" }";
    }

    @Override
    public String requiredPermission() { return "task:update"; }

    @Override
    public boolean requiresApproval() { return true; }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        Object id = args.get("id");
        if (id == null || id.toString().isBlank()) {
            throw new IllegalArgumentException("'id' is required");
        }
        UUID taskId;
        try {
            taskId = UUID.fromString(id.toString().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid task id");
        }
        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (args.get("status") != null && !args.get("status").toString().isBlank()) {
            task.setStatus(args.get("status").toString().trim());
        }
        if (args.get("assignedTo") != null && !args.get("assignedTo").toString().isBlank()) {
            task.setAssignedTo(args.get("assignedTo").toString().trim());
        }
        if (args.get("priority") != null && !args.get("priority").toString().isBlank()) {
            String p = args.get("priority").toString().trim().toUpperCase();
            if (!java.util.List.of("LOW", "MEDIUM", "HIGH").contains(p)) {
                throw new IllegalArgumentException("priority must be LOW, MEDIUM or HIGH");
            }
            task.setPriority(p);
        }
        taskRepository.save(task);
        return AgentToolResult.success("Task updated: " + task.getId() + " status=" + task.getStatus());
    }
}

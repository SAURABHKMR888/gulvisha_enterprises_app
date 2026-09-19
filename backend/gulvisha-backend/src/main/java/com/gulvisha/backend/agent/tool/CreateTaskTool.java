package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.task.Task;
import com.gulvisha.backend.task.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates a task in the agent's own organization.
 * Args: {title: string, description?: string, assignedTo?: string, priority?: LOW|MEDIUM|HIGH}
 */
@Component
public class CreateTaskTool implements AgentTool {

    private final TaskRepository taskRepository;

    public CreateTaskTool(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public String name() { return "create_task"; }

    @Override
    public String description() {
        return "Create a new task in this organization's task list.";
    }

    @Override
    public String argumentSpec() {
        return "{ \"title\": \"required task title\", \"description\": \"optional details\", "
             + "\"assignedTo\": \"optional username\", \"priority\": \"optional LOW|MEDIUM|HIGH\" }";
    }

    @Override
    public String requiredPermission() { return "task:create"; }

    @Override
    public boolean requiresApproval() { return true; }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        Object title = args.get("title");
        if (title == null || title.toString().isBlank()) {
            throw new IllegalArgumentException("'title' is required");
        }
        Task task = new Task(organizationId, null, title.toString().trim(),
                args.get("description") != null ? args.get("description").toString() : null);
        if (args.get("assignedTo") != null && !args.get("assignedTo").toString().isBlank()) {
            task.setAssignedTo(args.get("assignedTo").toString().trim());
        }
        if (args.get("priority") != null) {
            String p = args.get("priority").toString().trim().toUpperCase();
            if (!List.of("LOW", "MEDIUM", "HIGH").contains(p)) {
                throw new IllegalArgumentException("priority must be LOW, MEDIUM or HIGH");
            }
            task.setPriority(p);
        }
        Task saved = taskRepository.save(task);
        return AgentToolResult.success("Task created (id=" + saved.getId() + "): " + saved.getTitle());
    }
}

package com.gulvisha.backend.controller;

import com.gulvisha.backend.task.Task;
import com.gulvisha.backend.task.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@PreAuthorize("hasAnyAuthority('PERMISSION_task:view', 'PERMISSION_task:create', 'PERMISSION_task:update', 'PERMISSION_task:delete')")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Page<Task> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo) {
        return taskService.getTasks(PageRequest.of(page, size), projectId, status, assignedTo);
    }

    @GetMapping("/{id}")
    public Task getById(@PathVariable UUID id) {
        return taskService.getTaskById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_task:create')")
    public ResponseEntity<Task> create(@RequestBody TaskService.TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_task:update')")
    public Task update(@PathVariable UUID id, @RequestBody TaskService.TaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERMISSION_task:update')")
    public Task updateStatus(@PathVariable UUID id, @RequestBody StatusUpdateRequest request) {
        return taskService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_task:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    public record StatusUpdateRequest(String status) {}
}

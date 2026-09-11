package com.gulvisha.backend.controller;

import com.gulvisha.backend.security.UserContext;
import com.gulvisha.backend.project.Project;
import com.gulvisha.backend.project.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasAnyAuthority('PERMISSION_project:view', 'PERMISSION_project:create', 'PERMISSION_project:update', 'PERMISSION_project:delete')")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public Page<Project> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return projectService.getProjects(PageRequest.of(page, size), status, search);
    }

    @GetMapping("/{id}")
    public Project getById(@PathVariable UUID id) {
        return projectService.getProjectById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_project:create')")
    public ResponseEntity<Project> create(@RequestBody ProjectService.ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_project:update')")
    public Project update(@PathVariable UUID id, @RequestBody ProjectService.ProjectRequest request) {
        return projectService.updateProject(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERMISSION_project:update')")
    public Project updateStatus(@PathVariable UUID id, @RequestBody StatusUpdateRequest request) {
        return projectService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_project:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    public record StatusUpdateRequest(String status) {}
}

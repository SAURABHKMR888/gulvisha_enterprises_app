package com.gulvisha.backend.controller;

import com.gulvisha.backend.security.UserContext;
import com.gulvisha.backend.workflow.Workflow;
import com.gulvisha.backend.workflow.WorkflowEngine;
import com.gulvisha.backend.workflow.WorkflowExecution;
import com.gulvisha.backend.workflow.WorkflowExecutionRepository;
import com.gulvisha.backend.workflow.WorkflowRepository;
import com.gulvisha.backend.workflow.WorkflowStep;
import com.gulvisha.backend.workflow.WorkflowStepRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Workflow configuration and monitoring APIs.
 * Tenant isolation: every operation is scoped to the caller's organizationId from UserContext.
 */
@RestController
@RequestMapping("/api/workflows")
@PreAuthorize("hasAnyAuthority('PERMISSION_workflow:view', 'PERMISSION_workflow:manage')")
public class WorkflowController {

    public record WorkflowRequest(String name, String description, String triggerType, boolean enabled) {}

    public record StepRequest(@NotBlank String actionType, String name, Integer sortOrder, String config) {}

    public record RunRequest(UUID entityId) {}

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowEngine workflowEngine;

    public WorkflowController(WorkflowRepository workflowRepository,
                              WorkflowStepRepository stepRepository,
                              WorkflowExecutionRepository executionRepository,
                              WorkflowEngine workflowEngine) {
        this.workflowRepository = workflowRepository;
        this.stepRepository = stepRepository;
        this.executionRepository = executionRepository;
        this.workflowEngine = workflowEngine;
    }

    @GetMapping
    public List<Workflow> list() {
        return workflowRepository.findAllByOrganizationIdOrderByCreatedAtDesc(UserContext.getOrganizationId());
    }

    /**
     * Registry of available workflow triggers and step-action types.
     * Drives the workflow configuration UI so it stays in sync with the engine.
     */
    @GetMapping("/registry")
    public WorkflowRegistry registry() {
        return new WorkflowRegistry(
                List.of("ENQUIRY_CREATED", "LEAD_CREATED", "PROJECT_CREATED", "TASK_CREATED", "MANUAL"),
                workflowEngine.actionTypes()
        );
    }

    public record WorkflowRegistry(List<String> triggers, List<String> actions) {
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> get(@PathVariable UUID id) {
        return workflowRepository.findById(id)
                .filter(w -> w.getOrganizationId().equals(UserContext.getOrganizationId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_workflow:manage')")
    public Workflow create(@Valid @RequestBody WorkflowRequest request) {
        Workflow workflow = new Workflow(UserContext.getOrganizationId(), request.name());
        workflow.setDescription(request.description());
        workflow.setTriggerType(request.triggerType());
        workflow.setEnabled(request.enabled());
        return workflowRepository.save(workflow);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_workflow:manage')")
    public ResponseEntity<Workflow> update(@PathVariable UUID id, @Valid @RequestBody WorkflowRequest request) {
        return workflowRepository.findById(id)
                .filter(w -> w.getOrganizationId().equals(UserContext.getOrganizationId()))
                .map(w -> {
                    w.setName(request.name());
                    w.setDescription(request.description());
                    w.setTriggerType(request.triggerType());
                    w.setEnabled(request.enabled());
                    return ResponseEntity.ok(workflowRepository.save(w));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_workflow:manage')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return workflowRepository.findById(id)
                .filter(w -> w.getOrganizationId().equals(UserContext.getOrganizationId()))
                .map(w -> {
                    workflowRepository.delete(w);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/steps")
    public List<WorkflowStep> steps(@PathVariable UUID id) {
        return workflowRepository.findById(id)
                .filter(w -> w.getOrganizationId().equals(UserContext.getOrganizationId()))
                .map(w -> stepRepository.findByWorkflowIdOrderBySortOrderAsc(w.getId()))
                .orElse(List.of());
    }

    @PostMapping("/{id}/steps")
    @PreAuthorize("hasAuthority('PERMISSION_workflow:manage')")
    public ResponseEntity<WorkflowStep> addStep(@PathVariable UUID id, @Valid @RequestBody StepRequest request) {
        return workflowRepository.findById(id)
                .filter(w -> w.getOrganizationId().equals(UserContext.getOrganizationId()))
                .map(w -> {
                    WorkflowStep step = new WorkflowStep(
                            w.getOrganizationId(), w.getId(), request.name(), request.actionType(), request.config(),
                            request.sortOrder() != null ? request.sortOrder() : 0);
                    return ResponseEntity.ok(stepRepository.save(step));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/steps/{stepId}")
    @PreAuthorize("hasAuthority('PERMISSION_workflow:manage')")
    public ResponseEntity<Void> deleteStep(@PathVariable UUID stepId) {
        return stepRepository.findById(stepId)
                .filter(s -> workflowRepository.findById(s.getWorkflowId())
                        .map(w -> w.getOrganizationId().equals(UserContext.getOrganizationId()))
                        .orElse(false))
                .map(s -> {
                    stepRepository.delete(s);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/run")
    @PreAuthorize("hasAuthority('PERMISSION_workflow:manage')")
    public WorkflowExecution run(@PathVariable UUID id, @RequestBody RunRequest request) {
        return workflowEngine.runWorkflow(UserContext.getOrganizationId(), id, request.entityId());
    }

    @GetMapping("/{id}/executions")
    public List<WorkflowExecution> executions(@PathVariable UUID id) {
        return workflowRepository.findById(id)
                .filter(w -> w.getOrganizationId().equals(UserContext.getOrganizationId()))
                .map(w -> executionRepository.findAllByOrganizationIdAndWorkflowIdOrderByStartedAtDesc(
                        UserContext.getOrganizationId(), w.getId()))
                .orElse(List.of());
    }
}

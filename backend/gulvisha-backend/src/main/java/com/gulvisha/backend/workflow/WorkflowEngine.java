package com.gulvisha.backend.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Executes workflows: resolves enabled workflows for a trigger, runs their steps
 * in order via registered {@link WorkflowAction} handlers, and records an
 * execution history entry (status + step log) for observability.
 *
 * Trigger execution is failure-safe by design: a workflow error must never
 * break the business operation that fired it (e.g. enquiry submission).
 */
@Service
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final Map<String, WorkflowAction> actions = new LinkedHashMap<>();

    public WorkflowEngine(WorkflowRepository workflowRepository,
                          WorkflowStepRepository stepRepository,
                          WorkflowExecutionRepository executionRepository,
                          List<WorkflowAction> actionBeans) {
        this.workflowRepository = workflowRepository;
        this.stepRepository = stepRepository;
        this.executionRepository = executionRepository;
        actionBeans.forEach(action -> actions.put(action.type(), action));
    }

    /** Runs all enabled workflows for a trigger type. Never throws. */
    public void executeForTrigger(UUID organizationId, String triggerType, UUID entityId) {
        try {
            List<Workflow> workflows = workflowRepository
                    .findByOrganizationIdAndTriggerTypeAndEnabledTrue(organizationId, triggerType);
            for (Workflow workflow : workflows) {
                runWorkflow(organizationId, workflow, entityId);
            }
        } catch (Exception e) {
            log.error("Workflow trigger {} failed for entity {}: {}", triggerType, entityId, e.getMessage());
        }
    }

    /** Runs one specific workflow (manual run). Returns the execution record. */
    public WorkflowExecution runWorkflow(UUID organizationId, UUID workflowId, UUID entityId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .filter(w -> w.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
        return runWorkflow(organizationId, workflow, entityId);
    }

    private WorkflowExecution runWorkflow(UUID organizationId, Workflow workflow, UUID entityId) {
        WorkflowExecution execution = executionRepository.save(
                new WorkflowExecution(organizationId, workflow.getId(), workflow.getTriggerType(), entityId));
        StringBuilder logLines = new StringBuilder();
        try {
            List<WorkflowStep> steps = stepRepository.findByWorkflowIdOrderBySortOrderAsc(workflow.getId());
            for (WorkflowStep step : steps) {
                WorkflowAction action = actions.get(step.getActionType());
                if (action == null) {
                    throw new IllegalStateException("No action handler registered for type: " + step.getActionType());
                }
                String stepName = step.getName() != null && !step.getName().isBlank()
                        ? step.getName()
                        : step.getActionType();
                logLines.append("[").append(stepName).append("] ")
                        .append(action.execute(organizationId, entityId, step.getConfig()))
                        .append("\n");
            }
            execution.markCompleted(logLines.toString());
        } catch (Exception e) {
            logLines.append("FAILED: ").append(e.getMessage());
            execution.markFailed(logLines.toString());
            log.warn("Workflow {} execution failed: {}", workflow.getId(), e.getMessage());
        }
        return executionRepository.save(execution);
    }
}

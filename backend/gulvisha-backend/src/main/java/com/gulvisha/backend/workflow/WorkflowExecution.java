package com.gulvisha.backend.workflow;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID workflowId;

    @Column(length = 50)
    private String triggerType;

    @Column
    private UUID entityId;

    @Column(nullable = false, length = 20)
    private String status = "RUNNING";

    @Column(columnDefinition = "TEXT")
    private String log;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    @Column
    private Instant finishedAt;

    public WorkflowExecution() {
    }

    public WorkflowExecution(UUID organizationId, UUID workflowId, String triggerType, UUID entityId) {
        this.organizationId = organizationId;
        this.workflowId = workflowId;
        this.triggerType = triggerType;
        this.entityId = entityId;
        this.startedAt = Instant.now();
    }

    public void markCompleted(String log) {
        this.status = "COMPLETED";
        this.log = log;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String log) {
        this.status = "FAILED";
        this.log = log;
        this.finishedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getWorkflowId() { return workflowId; }
    public String getTriggerType() { return triggerType; }
    public UUID getEntityId() { return entityId; }
    public String getStatus() { return status; }
    public String getLog() { return log; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}

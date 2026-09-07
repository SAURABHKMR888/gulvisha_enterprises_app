package com.gulvisha.backend.workflow;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "workflow_steps")
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID workflowId;

    @Column(length = 150)
    private String name;

    /** Action handler key, e.g. CREATE_LEAD, ADD_NOTE. Resolved against WorkflowAction beans. */
    @Column(nullable = false, length = 50)
    private String actionType;

    /** Optional configuration for the action, e.g. note text. */
    @Column(length = 1000)
    private String config;

    @Column(nullable = false)
    private int sortOrder;

    public WorkflowStep() {
    }

    public WorkflowStep(UUID organizationId, UUID workflowId, String name, String actionType, String config, int sortOrder) {
        this.organizationId = organizationId;
        this.workflowId = workflowId;
        this.name = name;
        this.actionType = actionType;
        this.config = config;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getWorkflowId() { return workflowId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}

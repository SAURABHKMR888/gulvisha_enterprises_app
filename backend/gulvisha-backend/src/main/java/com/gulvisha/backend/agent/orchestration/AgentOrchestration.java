package com.gulvisha.backend.agent.orchestration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-scoped multi-agent workflow definition (Phase 14).
 * Steps reference worker Agent ids by UUID (stored as JSON array string).
 * Generic — no hardcoded agent names.
 */
@Entity
@Table(name = "ai_agent_orchestrations")
public class AgentOrchestration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    /** Optional supervisor agent id (may be null = orchestrator-driven sequencing). */
    @Column
    private UUID supervisorAgentId;

    /**
     * JSON array of steps, e.g.
     * [{"agentId":"...","mode":"SEQUENTIAL","condition":"output contains X","maxRetries":1,"timeoutSeconds":120}]
     */
    @Column(nullable = false, length = 8000)
    private String stepsJson = "[]";

    /**
     * SEQUENTIAL | PARALLEL_SAFE | CONDITIONAL.
     * SEQUENTIAL runs steps in order handing context along; PARALLEL_SAFE runs
     * independent steps concurrently on a bounded pool; CONDITIONAL skips steps
     * whose condition does not match the handoff context.
     */
    @Column(nullable = false, length = 20)
    private String executionMode = "SEQUENTIAL";

    @Column(nullable = false)
    private int maxSteps = 10;

    @Column(nullable = false)
    private int maxRetriesPerStep = 1;

    @Column(nullable = false)
    private int defaultTimeoutSeconds = 180;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    protected AgentOrchestration() {}

    public AgentOrchestration(UUID organizationId, String name) {
        this.organizationId = organizationId;
        this.name = name;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    private void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public UUID getSupervisorAgentId() { return supervisorAgentId; }
    public void setSupervisorAgentId(UUID id) { this.supervisorAgentId = id; }
    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String s) { this.stepsJson = s; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String m) { this.executionMode = m; }
    public int getMaxSteps() { return maxSteps; }
    public void setMaxSteps(int n) { this.maxSteps = n; }
    public int getMaxRetriesPerStep() { return maxRetriesPerStep; }
    public void setMaxRetriesPerStep(int n) { this.maxRetriesPerStep = n; }
    public int getDefaultTimeoutSeconds() { return defaultTimeoutSeconds; }
    public void setDefaultTimeoutSeconds(int n) { this.defaultTimeoutSeconds = n; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

package com.gulvisha.backend.agent;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Persisted record of a single agent execution (Phase 13).
 * stepsJson captures the ordered tool calls + results for audit/debug.
 */
@Entity
@Table(name = "ai_agent_runs")
public class AgentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID agentId;

    @Column(nullable = false, length = 200)
    private String agentName;

    @Column(nullable = false, length = 4000)
    private String input;

    @Column(length = 8000)
    private String output;

    @Column(length = 8000)
    private String stepsJson;

    @Column(length = 100)
    private String username;

    @Column(length = 100)
    private String model;

    @Column
    private Integer tokensUsed = 0;

    @Column
    private Long estimatedCostMicros = 0L;

    /** RUNNING | COMPLETED | FAILED | MAX_ITERATIONS | WAITING_APPROVAL | REJECTED */
    @Column(nullable = false, length = 20)
    private String status = "RUNNING";

    @Column(length = 2000)
    private String error;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant finishedAt;

    protected AgentRun() {}

    public AgentRun(UUID organizationId, UUID agentId, String agentName, String input) {
        this.organizationId = organizationId;
        this.agentId = agentId;
        this.agentName = agentName;
        this.input = input;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public String getInput() { return input; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String stepsJson) { this.stepsJson = stepsJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
    public Long getEstimatedCostMicros() { return estimatedCostMicros; }
    public void setEstimatedCostMicros(Long v) { this.estimatedCostMicros = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}

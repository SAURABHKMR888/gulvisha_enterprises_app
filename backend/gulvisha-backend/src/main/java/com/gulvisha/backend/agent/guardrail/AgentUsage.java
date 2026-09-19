package com.gulvisha.backend.agent.guardrail;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Per-tenant AI usage ledger (Phase 13 §8, preparation for Phase 17 billing).
 * One row per agent run / orchestration step that consumed model tokens.
 * Cost is an estimate in USD micros to avoid floating point drift.
 */
@Entity
@Table(name = "ai_agent_usage")
public class AgentUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(length = 100)
    private String username;

    @Column
    private UUID agentId;

    @Column(length = 200)
    private String agentName;

    @Column
    private UUID agentRunId;

    @Column
    private UUID orchestrationRunId;

    @Column(length = 100)
    private String model;

    @Column(nullable = false)
    private int tokensUsed = 0;

    /** Estimated cost in USD micro-units (1_000_000 = $1). */
    @Column(nullable = false)
    private long estimatedCostMicros = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentUsage() {}

    public AgentUsage(UUID organizationId, String username, UUID agentId, String agentName,
                      UUID agentRunId, UUID orchestrationRunId, String model,
                      int tokensUsed, long estimatedCostMicros) {
        this.organizationId = organizationId;
        this.username = username;
        this.agentId = agentId;
        this.agentName = agentName;
        this.agentRunId = agentRunId;
        this.orchestrationRunId = orchestrationRunId;
        this.model = model;
        this.tokensUsed = tokensUsed;
        this.estimatedCostMicros = estimatedCostMicros;
        this.createdAt = Instant.now();
    }

    @PrePersist
    private void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getUsername() { return username; }
    public UUID getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public UUID getAgentRunId() { return agentRunId; }
    public UUID getOrchestrationRunId() { return orchestrationRunId; }
    public String getModel() { return model; }
    public int getTokensUsed() { return tokensUsed; }
    public long getEstimatedCostMicros() { return estimatedCostMicros; }
    public Instant getCreatedAt() { return createdAt; }
}

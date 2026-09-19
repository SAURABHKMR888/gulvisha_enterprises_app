package com.gulvisha.backend.agent.guardrail;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Human approval gate for high-impact agent tool calls (Phase 13 §7,
 * reused by Phase 14 orchestration). A tool execution that requires approval
 * creates a PENDING row; the run pauses until a human APPROVES or REJECTS it.
 */
@Entity
@Table(name = "ai_agent_approvals")
public class AgentApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    /** Single-agent run that requested approval (null for orchestration steps). */
    @Column
    private UUID agentRunId;

    /** Orchestration run that requested approval (null for single-agent runs). */
    @Column
    private UUID orchestrationRunId;

    @Column(nullable = false)
    private UUID agentId;

    @Column(nullable = false, length = 200)
    private String agentName;

    @Column(nullable = false, length = 120)
    private String toolName;

    /** JSON-encoded tool args (truncated, never log secrets). */
    @Column(length = 4000)
    private String argsJson;

    @Column(length = 100)
    private String requestedBy;

    /** PENDING | APPROVED | REJECTED | EXPIRED */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(length = 100)
    private String decidedBy;

    @Column
    private Instant decidedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentApproval() {}

    public AgentApproval(UUID organizationId, UUID agentRunId, UUID orchestrationRunId,
                         UUID agentId, String agentName, String toolName,
                         String argsJson, String requestedBy) {
        this.organizationId = organizationId;
        this.agentRunId = agentRunId;
        this.orchestrationRunId = orchestrationRunId;
        this.agentId = agentId;
        this.agentName = agentName;
        this.toolName = toolName;
        this.argsJson = argsJson;
        this.requestedBy = requestedBy;
        this.createdAt = Instant.now();
    }

    @PrePersist
    private void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "PENDING";
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getAgentRunId() { return agentRunId; }
    public UUID getOrchestrationRunId() { return orchestrationRunId; }
    public UUID getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public String getToolName() { return toolName; }
    public String getArgsJson() { return argsJson; }
    public String getRequestedBy() { return requestedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
}

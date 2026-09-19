package com.gulvisha.backend.agent.orchestration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Persisted record of one orchestration execution (Phase 14 §9 observability).
 */
@Entity
@Table(name = "ai_orchestration_runs")
public class OrchestrationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID orchestrationId;

    @Column(nullable = false, length = 200)
    private String orchestrationName;

    @Column(length = 100)
    private String username;

    @Column(nullable = false, length = 4000)
    private String input;

    @Column(length = 8000)
    private String output;

    /** JSON array of per-step records: agent, handoff, tools, retries, duration. */
    @Column(length = 12000)
    private String stepsJson;

    /** RUNNING | COMPLETED | FAILED | WAITING_APPROVAL | REJECTED */
    @Column(nullable = false, length = 20)
    private String status = "RUNNING";

    @Column(length = 2000)
    private String error;

    @Column(nullable = false)
    private int totalTokens = 0;

    @Column(nullable = false)
    private long estimatedCostMicros = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant finishedAt;

    protected OrchestrationRun() {}

    public OrchestrationRun(UUID organizationId, UUID orchestrationId,
                            String orchestrationName, String username, String input) {
        this.organizationId = organizationId;
        this.orchestrationId = orchestrationId;
        this.orchestrationName = orchestrationName;
        this.username = username;
        this.input = input;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getOrchestrationId() { return orchestrationId; }
    public String getOrchestrationName() { return orchestrationName; }
    public String getUsername() { return username; }
    public String getInput() { return input; }
    public String getOutput() { return output; }
    public void setOutput(String o) { this.output = o; }
    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String s) { this.stepsJson = s; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getError() { return error; }
    public void setError(String e) { this.error = e; }
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int n) { this.totalTokens = n; }
    public long getEstimatedCostMicros() { return estimatedCostMicros; }
    public void setEstimatedCostMicros(long n) { this.estimatedCostMicros = n; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant t) { this.finishedAt = t; }
}

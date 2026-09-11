package com.gulvisha.backend.agent;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A tenant-scoped AI agent definition (Phase 13).
 * An Agent bundles a system prompt, the model settings it runs with and
 * the set of tool names it is allowed to call (see {@link AgentTool}).
 */
@Entity
@Table(name = "ai_agents")
public class Agent {

    public static final int MAX_TOOLS = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 4000)
    private String systemPrompt;

    /** Comma-separated tool names this agent may call. Empty/blank = no tools (pure Q&A). */
    @Column(length = 1000)
    private String allowedTools;

    @Column(length = 50)
    private String provider;   // optional override; null = org default AI config

    @Column(length = 100)
    private String model;      // optional override; null = org default

    @Column
    private Double temperature;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    protected Agent() {}

    public Agent(UUID organizationId, String name, String description, String systemPrompt, String allowedTools) {
        this.organizationId = organizationId;
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.allowedTools = allowedTools;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    private void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getAllowedTools() { return allowedTools; }
    public void setAllowedTools(String allowedTools) { this.allowedTools = allowedTools; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

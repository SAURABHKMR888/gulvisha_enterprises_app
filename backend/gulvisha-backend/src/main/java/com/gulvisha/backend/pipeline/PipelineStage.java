package com.gulvisha.backend.pipeline;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipeline_stages")
public class PipelineStage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID organizationId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false)
    private int sortOrder;
    
    @Column(nullable = false)
    private boolean defaultStage = false;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    protected PipelineStage() {}
    
    public PipelineStage(UUID organizationId, String name, int sortOrder, boolean defaultStage) {
        this.organizationId = organizationId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.defaultStage = defaultStage;
        this.createdAt = Instant.now();
    }
    
    @PrePersist
    private void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
    public boolean isDefaultStage() { return defaultStage; }
    public Instant getCreatedAt() { return createdAt; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setDefaultStage(boolean defaultStage) { this.defaultStage = defaultStage; }
}
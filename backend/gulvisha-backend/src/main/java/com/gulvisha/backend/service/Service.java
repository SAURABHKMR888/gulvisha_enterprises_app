package com.gulvisha.backend.service;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String status = "ACTIVE";

    @Column(length = 100)
    private String pricingInfo;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Service() {
    }

    public Service(UUID organizationId, String name) {
        this.organizationId = organizationId;
        this.name = name;
        this.createdAt = Instant.now();
    }

    @PrePersist
    private void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPricingInfo() { return pricingInfo; }
    public void setPricingInfo(String pricingInfo) { this.pricingInfo = pricingInfo; }
    public Instant getCreatedAt() { return createdAt; }
}

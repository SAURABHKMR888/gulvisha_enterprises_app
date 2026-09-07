package com.gulvisha.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quote_requests")
public class QuoteRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 120)
    private String company;

    @Column(nullable = false, length = 80)
    private String service;

    @Column(nullable = false, length = 2000)
    private String details;

    @Column(length = 30)
    private String phone;

    @Column(length = 80)
    private String serviceCategory;

    @Column(length = 80)
    private String budget;

    @Column(length = 80)
    private String timeline;

    @Column(length = 20)
    private String source;

    @Column(length = 30)
    private String preferredContactMethod;

    @Column(nullable = false, length = 30)
    private String status = "NEW";

    @Column(length = 4000)
    private String internalNotes;

    @Column(nullable = false, updatable = false)
    private Instant receivedAt;

    protected QuoteRequestEntity() {
    }

    public QuoteRequestEntity(String name, String email, String company, String service, String details) {
        this(null, name, email, company, service, details, null, null, null, null, "WEBSITE", null);
    }

    public QuoteRequestEntity(UUID organizationId, String name, String email, String company, String service, String details) {
        this(organizationId, name, email, company, service, details, null, null, null, null, "WEBSITE", null);
    }

    public QuoteRequestEntity(String name, String email, String company, String service, String details,
                              String phone, String serviceCategory, String budget, String timeline, String source,
                              String preferredContactMethod) {
        this(null, name, email, company, service, details, phone, serviceCategory, budget, timeline, source, preferredContactMethod);
    }

    public QuoteRequestEntity(UUID organizationId, String name, String email, String company, String service, String details,
                              String phone, String serviceCategory, String budget, String timeline, String source,
                              String preferredContactMethod) {
        this.organizationId = organizationId;
        this.name = name;
        this.email = email;
        this.company = company;
        this.service = service;
        this.details = details;
        this.phone = phone;
        this.serviceCategory = serviceCategory;
        this.budget = budget;
        this.timeline = timeline;
        this.source = source;
        this.preferredContactMethod = preferredContactMethod;
        this.status = "NEW";
        this.receivedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    private void ensureStatus() {
        if (status == null || status.isBlank()) {
            this.status = "NEW";
        }
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCompany() { return company; }
    public String getService() { return service; }
    public String getDetails() { return details; }
    public String getPhone() { return phone; }
    public String getServiceCategory() { return serviceCategory; }
    public String getBudget() { return budget; }
    public String getTimeline() { return timeline; }
    public String getSource() { return source; }
    public String getPreferredContactMethod() { return preferredContactMethod; }
    public String getStatus() { return status; }
    public String getInternalNotes() { return internalNotes; }
    public Instant getReceivedAt() { return receivedAt; }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }
}
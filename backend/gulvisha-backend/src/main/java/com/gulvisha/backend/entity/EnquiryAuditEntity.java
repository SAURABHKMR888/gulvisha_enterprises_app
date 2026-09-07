package com.gulvisha.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enquiry_audit")
public class EnquiryAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID enquiryId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(length = 4000)
    private String previousValue;

    @Column(length = 4000)
    private String newValue;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected EnquiryAuditEntity() {
    }

    public EnquiryAuditEntity(UUID enquiryId, String action, String previousValue, String newValue, String actor) {
        this.enquiryId = enquiryId;
        this.action = action;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.actor = actor;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEnquiryId() { return enquiryId; }
    public String getAction() { return action; }
    public String getPreviousValue() { return previousValue; }
    public String getNewValue() { return newValue; }
    public String getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
}
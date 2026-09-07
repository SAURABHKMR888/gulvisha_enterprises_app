package com.gulvisha.backend.customfield;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "custom_field_definitions")
public class CustomFieldDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID organizationId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 50)
    private String entityType; // "LEAD", "CLIENT", "PROJECT", "CONTACT"
    
    @Column(nullable = false, length = 50)
    private String fieldType; // "TEXT", "NUMBER", "DATE", "BOOL", "SELECT"
    
    @Column(length = 500)
    private String options; // comma-separated for SELECT type
    
    @Column(nullable = false)
    private boolean required = false;
    
    private boolean enabled = true;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    protected CustomFieldDefinition() {}
    
    public CustomFieldDefinition(UUID organizationId, String name, String entityType, String fieldType, String options, boolean required) {
        this.organizationId = organizationId;
        this.name = name;
        this.entityType = entityType;
        this.fieldType = fieldType;
        this.options = options;
        this.required = required;
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
    public String getEntityType() { return entityType; }
    public String getFieldType() { return fieldType; }
    public String getOptions() { return options; }
    public boolean isRequired() { return required; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public void setOptions(String options) { this.options = options; }
    public void setRequired(boolean required) { this.required = required; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
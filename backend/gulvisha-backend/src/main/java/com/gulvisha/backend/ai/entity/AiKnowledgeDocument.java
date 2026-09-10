package com.gulvisha.backend.ai.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A knowledge base document ingested for retrieval-augmented generation (RAG).
 * The raw content is split into {@link AiKnowledgeChunk} rows that carry embeddings.
 */
@Entity
@Table(name = "ai_knowledge_documents")
public class AiKnowledgeDocument {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_EMBEDDING = "EMBEDDING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String status = STATUS_PENDING;

    @Column(nullable = false)
    private int chunkCount = 0;

    @Column(length = 500)
    private String error;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public AiKnowledgeDocument() {
    }

    public AiKnowledgeDocument(UUID organizationId, String title, String content) {
        this.organizationId = organizationId;
        this.title = title;
        this.content = content;
        this.status = STATUS_PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    private void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

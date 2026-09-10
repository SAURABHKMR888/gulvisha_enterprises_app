package com.gulvisha.backend.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One chunk of a knowledge document with its embedding vector.
 * The embedding is stored as JSONB (a float array) — at small-business scale,
 * in-memory cosine similarity over an organization's chunks is sufficient and
 * requires no vector database extension.
 */
@Entity
@Table(name = "ai_knowledge_chunks")
public class AiKnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private float[] embedding;

    @Column(nullable = false, length = 100)
    private String embeddingModel;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public AiKnowledgeChunk() {
    }

    public AiKnowledgeChunk(UUID organizationId, UUID documentId, int chunkIndex, String content) {
        this.organizationId = organizationId;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.createdAt = Instant.now();
    }

    @PrePersist
    private void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public Instant getCreatedAt() { return createdAt; }
}

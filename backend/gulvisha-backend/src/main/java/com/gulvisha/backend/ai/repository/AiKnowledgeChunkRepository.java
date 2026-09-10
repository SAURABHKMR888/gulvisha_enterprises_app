package com.gulvisha.backend.ai.repository;

import com.gulvisha.backend.ai.entity.AiKnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiKnowledgeChunkRepository extends JpaRepository<AiKnowledgeChunk, UUID> {
    List<AiKnowledgeChunk> findAllByOrganizationId(UUID organizationId);
    List<AiKnowledgeChunk> findAllByDocumentIdOrderByChunkIndexAsc(UUID documentId);
    long countByDocumentId(UUID documentId);
    void deleteByDocumentId(UUID documentId);
}

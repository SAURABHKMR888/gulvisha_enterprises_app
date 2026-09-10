package com.gulvisha.backend.ai.repository;

import com.gulvisha.backend.ai.entity.AiKnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiKnowledgeDocumentRepository extends JpaRepository<AiKnowledgeDocument, UUID> {
    List<AiKnowledgeDocument> findAllByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);
    Optional<AiKnowledgeDocument> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);
}

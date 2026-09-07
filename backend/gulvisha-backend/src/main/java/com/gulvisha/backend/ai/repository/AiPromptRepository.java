package com.gulvisha.backend.ai.repository;

import com.gulvisha.backend.ai.entity.AiPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiPromptRepository extends JpaRepository<AiPrompt, UUID> {
    List<AiPrompt> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<AiPrompt> findByOrganizationIdAndTypeOrderByCreatedAtDesc(UUID organizationId, String type);
}

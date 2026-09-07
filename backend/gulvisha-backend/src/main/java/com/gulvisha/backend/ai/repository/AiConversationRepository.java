package com.gulvisha.backend.ai.repository;

import com.gulvisha.backend.ai.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {
    List<AiConversation> findAllByOrganizationIdAndUserIdOrderByUpdatedAtDesc(UUID organizationId, String userId);
}

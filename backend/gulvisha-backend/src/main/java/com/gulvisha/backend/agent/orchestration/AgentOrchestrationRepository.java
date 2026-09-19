package com.gulvisha.backend.agent.orchestration;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentOrchestrationRepository extends JpaRepository<AgentOrchestration, UUID> {
    List<AgentOrchestration> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<AgentOrchestration> findByOrganizationIdAndId(UUID organizationId, UUID id);
}

package com.gulvisha.backend.agent;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID> {

    List<Agent> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    List<Agent> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<Agent> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Agent> findByOrganizationIdAndId(UUID organizationId, UUID id);
}

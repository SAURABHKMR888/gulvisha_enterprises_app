package com.gulvisha.backend.agent.orchestration;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrchestrationRunRepository extends JpaRepository<OrchestrationRun, UUID> {
    List<OrchestrationRun> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<OrchestrationRun> findAllByOrganizationIdAndOrchestrationIdOrderByCreatedAtDesc(UUID organizationId, UUID orchestrationId);
}

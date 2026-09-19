package com.gulvisha.backend.agent.guardrail;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AgentApprovalRepository extends JpaRepository<AgentApproval, UUID> {
    List<AgentApproval> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<AgentApproval> findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, String status);
    List<AgentApproval> findAllByOrganizationIdAndAgentRunIdOrderByCreatedAtDesc(UUID organizationId, UUID agentRunId);
    List<AgentApproval> findAllByOrganizationIdAndOrchestrationRunIdOrderByCreatedAtDesc(UUID organizationId, UUID orchestrationRunId);
}

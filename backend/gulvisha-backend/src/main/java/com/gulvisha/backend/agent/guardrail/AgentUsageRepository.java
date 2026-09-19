package com.gulvisha.backend.agent.guardrail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface AgentUsageRepository extends JpaRepository<AgentUsage, UUID> {
    List<AgentUsage> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    @Query("select coalesce(sum(u.tokensUsed),0) from AgentUsage u where u.organizationId = :orgId")
    long sumTokensByOrganizationId(UUID orgId);

    @Query("select coalesce(sum(u.estimatedCostMicros),0) from AgentUsage u where u.organizationId = :orgId")
    long sumCostMicrosByOrganizationId(UUID orgId);
}

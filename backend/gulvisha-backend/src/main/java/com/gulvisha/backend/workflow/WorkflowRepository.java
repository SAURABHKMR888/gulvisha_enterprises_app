package com.gulvisha.backend.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    List<Workflow> findAllByOrganizationId(UUID organizationId);

    List<Workflow> findByOrganizationIdAndTriggerTypeAndEnabledTrue(UUID organizationId, String triggerType);

    List<Workflow> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}

package com.gulvisha.backend.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    List<WorkflowExecution> findAllByOrganizationIdAndWorkflowIdOrderByStartedAtDesc(UUID organizationId, UUID workflowId);
}

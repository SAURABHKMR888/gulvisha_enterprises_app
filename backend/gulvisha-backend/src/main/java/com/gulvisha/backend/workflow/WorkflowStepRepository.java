package com.gulvisha.backend.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, UUID> {

    List<WorkflowStep> findByWorkflowIdOrderBySortOrderAsc(UUID workflowId);

    List<WorkflowStep> findAllByOrganizationIdAndWorkflowId(UUID organizationId, UUID workflowId);

    void deleteAllByWorkflowId(UUID workflowId);
}

package com.gulvisha.backend.agent.guardrail;

import com.gulvisha.backend.agent.orchestration.OrchestrationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Breaks the Approval -> Orchestration circular dependency.
 */
@Component
public class OrchestrationResumeBridge {

    private final OrchestrationService orchestrationService;

    public OrchestrationResumeBridge(@Lazy OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    public Object resume(UUID orgId, UUID approvalId) {
        return orchestrationService.resumeAfterApproval(orgId, approvalId);
    }

    public Object markRejected(UUID orgId, AgentApproval approval) {
        return orchestrationService.markRejectedAfterDecision(orgId, approval.getOrchestrationRunId(),
                approval.getDecidedBy(), approval.getToolName());
    }
}

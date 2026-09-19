package com.gulvisha.backend.agent.guardrail;

import com.gulvisha.backend.security.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Human-approval queue + usage API (Phase 13 §7–§8).
 * Tenant-scoped; approve/reject resumes paused runs.
 */
@RestController
@RequestMapping("/api/ai/approvals")
public class AgentApprovalController {

    private final AgentApprovalService approvalService;
    private final AgentResumeService resumeService;
    private final AgentGuardrailService guardrails;
    private final OrchestrationResumeBridge orchBridge;

    public AgentApprovalController(AgentApprovalService approvalService,
                                   AgentResumeService resumeService,
                                   AgentGuardrailService guardrails,
                                   OrchestrationResumeBridge orchBridge) {
        this.approvalService = approvalService;
        this.resumeService = resumeService;
        this.guardrails = guardrails;
        this.orchBridge = orchBridge;
    }

    @GetMapping
    public List<AgentApproval> list(@RequestParam(required = false) String status) {
        return approvalService.list(UserContext.getOrganizationId(), status);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID id) {
        return decide(id, true);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id) {
        return decide(id, false);
    }

    private ResponseEntity<?> decide(UUID id, boolean approve) {
        UUID orgId = UserContext.getOrganizationId();
        var approval = approvalService.decide(orgId, id, approve);
        if (!approve) {
            if (approval.getOrchestrationRunId() != null) {
                return ResponseEntity.ok(orchBridge.markRejected(orgId, approval));
            }
            return ResponseEntity.ok(resumeService.resumeAfterDecision(orgId, id));
        }
        if (approval.getOrchestrationRunId() != null) {
            return ResponseEntity.ok(orchBridge.resume(orgId, id));
        }
        return ResponseEntity.ok(resumeService.resumeAfterDecision(orgId, id));
    }

    @GetMapping("/usage")
    public Map<String, Object> usage() {
        UUID orgId = UserContext.getOrganizationId();
        return Map.of(
                "tokensUsed", guardrails.tenantTokensUsed(orgId),
                "costMicros", guardrails.tenantCostMicrosUsed(orgId));
    }
}

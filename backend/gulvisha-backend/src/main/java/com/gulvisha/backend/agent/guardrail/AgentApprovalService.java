package com.gulvisha.backend.agent.guardrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulvisha.backend.agent.Agent;
import com.gulvisha.backend.agent.AgentTool;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Human-approval queue (Phase 13 §7). High-impact tool calls create a
 * PENDING approval and pause the run; approve/reject resumes or aborts it.
 * All decisions are tenant-scoped and audited with decider + timestamp.
 */
@Service
public class AgentApprovalService {

    private final AgentApprovalRepository approvalRepository;
    private final ObjectMapper objectMapper;

    public AgentApprovalService(AgentApprovalRepository approvalRepository, ObjectMapper objectMapper) {
        this.approvalRepository = approvalRepository;
        this.objectMapper = objectMapper;
    }

    public AgentApproval requestApproval(UUID orgId, UUID agentRunId, UUID orchestrationRunId,
                                         Agent agent, AgentTool tool, Object args, String requestedBy) {
        String argsJson = truncate(toJson(args), 4000);
        AgentApproval approval = new AgentApproval(orgId, agentRunId, orchestrationRunId,
                agent.getId(), agent.getName(), tool.name(), argsJson, requestedBy);
        return approvalRepository.save(approval);
    }

    public java.util.List<AgentApproval> pending(UUID orgId) {
        return approvalRepository.findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(orgId, "PENDING");
    }

    public java.util.List<AgentApproval> list(UUID orgId, String status) {
        if (status == null || status.isBlank()) {
            return approvalRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId);
        }
        return approvalRepository.findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(orgId, status.toUpperCase());
    }

    public AgentApproval decide(UUID orgId, UUID approvalId, boolean approve) {
        AgentApproval approval = approvalRepository.findById(approvalId)
                .filter(a -> a.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found"));
        if (!"PENDING".equals(approval.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval already decided");
        }
        approval.setStatus(approve ? "APPROVED" : "REJECTED");
        approval.setDecidedBy(currentUsername());
        approval.setDecidedAt(Instant.now());
        return approvalRepository.save(approval);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}

package com.gulvisha.backend.agent.guardrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulvisha.backend.agent.Agent;
import com.gulvisha.backend.agent.AgentRun;
import com.gulvisha.backend.agent.AgentRunRepository;
import com.gulvisha.backend.agent.AgentService;
import com.gulvisha.backend.agent.AgentTool;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Resumes a paused single-agent run after a human decision (Phase 13 §7).
 * APPROVED: the approved tool executes, then the run continues its loop.
 * REJECTED: the run is marked REJECTED and stops.
 */
@Service
public class AgentResumeService {

    private final AgentApprovalRepository approvalRepository;
    private final AgentRunRepository runRepository;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;

    public AgentResumeService(AgentApprovalRepository approvalRepository,
                              AgentRunRepository runRepository,
                              AgentService agentService,
                              ObjectMapper objectMapper) {
        this.approvalRepository = approvalRepository;
        this.runRepository = runRepository;
        this.agentService = agentService;
        this.objectMapper = objectMapper;
    }

    public AgentRun resumeAfterDecision(UUID orgId, UUID approvalId) {
        AgentApproval approval = approvalRepository.findById(approvalId)
                .filter(a -> a.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found"));
        if (!"APPROVED".equals(approval.getStatus()) && !"REJECTED".equals(approval.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval is still pending");
        }
        if (approval.getAgentRunId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Orchestration approvals resume via the orchestration endpoint");
        }
        AgentRun run = runRepository.findById(approval.getAgentRunId())
                .filter(r -> r.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent run not found"));
        if (!"WAITING_APPROVAL".equals(run.getStatus())) {
            return run;
        }
        if ("REJECTED".equals(approval.getStatus())) {
            run.setStatus("REJECTED");
            run.setError("Rejected by " + approval.getDecidedBy() + ": tool " + approval.getToolName());
            run.setFinishedAt(Instant.now());
            return runRepository.save(run);
        }
        Agent agent = agentService.get(orgId, approval.getAgentId());
        AgentTool tool = agentService.tools().stream()
                .filter(t -> t.name().equals(approval.getToolName()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tool no longer available"));
        java.util.Map<String, Object> args;
        try {
            args = approval.getArgsJson() == null || approval.getArgsJson().isBlank()
                    ? new java.util.LinkedHashMap<>()
                    : objectMapper.readValue(approval.getArgsJson(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stored approval args are invalid");
        }
        return agentService.continueAfterApproval(orgId, agent, run, tool, args,
                approval.getDecidedBy());
    }
}

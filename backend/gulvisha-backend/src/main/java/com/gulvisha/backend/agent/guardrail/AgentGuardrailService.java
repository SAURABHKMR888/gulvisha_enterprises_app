package com.gulvisha.backend.agent.guardrail;

import com.gulvisha.backend.agent.Agent;
import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reusable guardrail layer (Phase 13 §4-§6), also enforced by the
 * Phase 14 orchestrator for EVERY worker invocation.
 */
@Component
public class AgentGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(AgentGuardrailService.class);

    static final Set<String> HIGH_IMPACT_TOOLS = Set.of(
            "create_lead", "update_lead", "create_task", "update_task",
            "create_ticket", "send_email", "send_notification");

    static final int MAX_INPUT_CHARS = 4000;
    static final int MAX_CONTEXT_CHARS = 8000;
    static final long TENANT_TOKEN_QUOTA = 1_000_000L;

    static final List<String> INJECTION_PATTERNS = List.of(
            "ignore previous instructions", "ignore all previous", "disregard previous",
            "system prompt", "reveal your prompt", "bypass permissions",
            "act as root", "act as admin", "sudo mode", "jailbreak");

    private final AgentUsageRepository usageRepository;

    public AgentGuardrailService(AgentUsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    public String checkInput(String input) {
        if (input == null || input.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Input must not be empty");
        }
        if (input.length() > MAX_INPUT_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Input too long (max " + MAX_INPUT_CHARS + " characters)");
        }
        String lowered = input.toLowerCase();
        for (String pattern : INJECTION_PATTERNS) {
            if (lowered.contains(pattern)) {
                log.warn("Input guardrail blocked prompt-injection attempt");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Input rejected by guardrail: unsafe instructions detected");
            }
        }
        return input.trim();
    }

    public String checkHandoffContext(String context) {
        if (context == null) return "";
        if (context.length() > MAX_CONTEXT_CHARS) {
            return context.substring(0, MAX_CONTEXT_CHARS);
        }
        return context;
    }

    public void checkInvocation(UUID orgId, Agent agent, String username) {
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No tenant context");
        }
        if (!orgId.equals(agent.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
        }
        if (!agent.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agent is disabled");
        }
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No user context");
        }
        long used = usageRepository.sumTokensByOrganizationId(orgId);
        if (used >= TENANT_TOKEN_QUOTA) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Tenant AI usage limit exceeded (" + used + " tokens used)");
        }
    }

    public AgentTool checkTool(List<AgentTool> allowed, String toolName) {
        return allowed.stream().filter(t -> t.name().equals(toolName)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Tool '" + toolName + "' is not enabled for this agent"));
    }

    public boolean requiresApproval(String toolName) {
        return HIGH_IMPACT_TOOLS.contains(toolName);
    }

    public String checkOutput(String output) {
        if (output == null || output.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Model produced empty output");
        }
        String lowered = output.toLowerCase();
        if (lowered.contains("<tool>") && !lowered.contains("</tool>")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Model produced malformed tool call");
        }
        return output;
    }

    public long tenantTokensUsed(UUID orgId) {
        return usageRepository.sumTokensByOrganizationId(orgId);
    }

    public long tenantCostMicrosUsed(UUID orgId) {
        return usageRepository.sumCostMicrosByOrganizationId(orgId);
    }

    public void recordUsage(UUID orgId, String username, Agent agent, UUID agentRunId,
                            UUID orchestrationRunId, String model, int tokens) {
        long costMicros = Math.max(0, (long) tokens * 2L);
        usageRepository.save(new AgentUsage(orgId, username,
                agent != null ? agent.getId() : null,
                agent != null ? agent.getName() : null,
                agentRunId, orchestrationRunId, model, tokens, costMicros));
    }

    public String currentUsername() {
        UserContext.CurrentUser u = UserContext.get();
        return u == null ? null : u.username();
    }
}

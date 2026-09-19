package com.gulvisha.backend.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulvisha.backend.agent.guardrail.AgentApproval;
import com.gulvisha.backend.agent.guardrail.AgentApprovalService;
import com.gulvisha.backend.agent.guardrail.AgentGuardrailService;
import com.gulvisha.backend.ai.entity.AiConfiguration;
import com.gulvisha.backend.ai.provider.AiChatRequest;
import com.gulvisha.backend.ai.provider.AiChatResponse;
import com.gulvisha.backend.ai.provider.AiProvider;
import com.gulvisha.backend.ai.provider.AiProviderFactory;
import com.gulvisha.backend.ai.repository.AiConfigurationRepository;
import com.gulvisha.backend.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final int MAX_ITERATIONS = 6;
    private static final int MAX_TOOL_OUTPUT_CHARS = 4000;

    private final AgentRepository agentRepository;
    private final AgentRunRepository runRepository;
    private final AgentToolRegistry toolRegistry;
    private final AiConfigurationRepository configRepository;
    private final AiProviderFactory providerFactory;
    private final AgentGuardrailService guardrails;
    private final AgentApprovalService approvals;
    private final ObjectMapper objectMapper;

    public AgentService(AgentRepository agentRepository,
                        AgentRunRepository runRepository,
                        AgentToolRegistry toolRegistry,
                        AiConfigurationRepository configRepository,
                        AiProviderFactory providerFactory,
                        AgentGuardrailService guardrails,
                        AgentApprovalService approvals,
                        ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.runRepository = runRepository;
        this.toolRegistry = toolRegistry;
        this.configRepository = configRepository;
        this.providerFactory = providerFactory;
        this.guardrails = guardrails;
        this.approvals = approvals;
        this.objectMapper = objectMapper;
    }

    public List<Agent> list(UUID orgId) {
        return agentRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId);
    }


    public Agent get(UUID orgId, UUID id) {
        return agentRepository.findByOrganizationIdAndId(orgId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
    }

    public Agent create(UUID orgId, Agent agent) {
        validateAllowedTools(agent.getAllowedTools());
        return agentRepository.save(agent);
    }

    public Agent update(UUID orgId, UUID id, Agent changes) {
        Agent agent = get(orgId, id);
        agent.setName(changes.getName());
        agent.setDescription(changes.getDescription());
        agent.setSystemPrompt(changes.getSystemPrompt());
        agent.setAllowedTools(changes.getAllowedTools());
        agent.setProvider(changes.getProvider());
        agent.setModel(changes.getModel());
        agent.setTemperature(changes.getTemperature());
        agent.setEnabled(changes.isEnabled());
        validateAllowedTools(agent.getAllowedTools());
        return agentRepository.save(agent);
    }

    public void delete(UUID orgId, UUID id) {
        agentRepository.delete(get(orgId, id));
    }

    private void validateAllowedTools(String allowedTools) {
        if (allowedTools == null || allowedTools.isBlank()) return;
        for (String name : allowedTools.split(",")) {
            String t = name.trim();
            if (!t.isEmpty() && toolRegistry.get(t) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tool: " + t);
            }
        }
    }


    // ---------- Run loop ----------

    public AgentRun run(UUID orgId, Agent agent, String input) {
        String username = currentUsername();
        String safeInput = guardrails.checkInput(input);
        guardrails.checkInvocation(orgId, agent, username);
        AiConfiguration config = configRepository.findByOrganizationId(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AI is not configured for this organization"));
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI is disabled in AI Configuration");
        }


        AgentRun run = runRepository.save(new AgentRun(orgId, agent.getId(), agent.getName(), safeInput));
        run.setUsername(username);
        run.setModel(agent.getModel() != null ? agent.getModel() : config.getModel());
        run = runRepository.save(run);
        List<AgentTool> allowed = agent.getAllowedTools() == null || agent.getAllowedTools().isBlank()
                ? List.of() : toolRegistry.resolveWhitelist(agent.getAllowedTools());

        List<Step> steps = new ArrayList<>();
        List<AiChatRequest.ChatMessage> history = new ArrayList<>();
        history.add(new AiChatRequest.ChatMessage("system", buildSystemPrompt(agent, allowed)));
        history.add(new AiChatRequest.ChatMessage("user", input));

        String finalAnswer = null;
        String status = "COMPLETED";
        String error = null;
        int totalTokens = 0;
        String usedModel = run.getModel();

        try {
            for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                AiProvider provider = providerFactory.getProvider(agent.getProvider() != null
                        ? agent.getProvider() : config.getProvider());
                AiChatRequest request = new AiChatRequest(
                        agent.getModel() != null ? agent.getModel() : config.getModel(),
                        history,
                        agent.getTemperature() != null ? agent.getTemperature() : config.getTemperature(),
                        config.getMaxTokens(),
                        config.getApiKey(),
                        config.getBaseUrl());
                AiChatResponse response = provider.chat(request);
                totalTokens += response.tokensUsed() != null ? response.tokensUsed() : 0;
                String content = guardrails.checkOutput(response.content());
                history.add(new AiChatRequest.ChatMessage("assistant", content));

                String toolName = extractTag(content, "tool");
                if (toolName == null) {
                    finalAnswer = content.replaceAll("\uCF80", "").trim();
                    break;
                }

                Map<String, Object> args = parseArgs(extractTag(content, "args"));
                AgentTool tool = guardrails.checkTool(allowed, toolName);
                assertUserPermission(tool);
                if (tool.requiresApproval() || guardrails.requiresApproval(toolName)) {
                    AgentApproval approval = approvals.requestApproval(orgId, run.getId(), null,
                            agent, tool, args, username);
                    run.setOutput("(waiting for human approval: " + toolName + ")");
                    run.setStepsJson(writeSteps(steps));
                    run.setStatus("WAITING_APPROVAL");
                    run.setError("Approval required: " + approval.getId());
                    run.setTokensUsed(totalTokens);
                    run.setFinishedAt(null);
                    guardrails.recordUsage(orgId, username, agent, run.getId(), null, usedModel, totalTokens);
                    return runRepository.save(run);
                }
                String result;
                boolean ok;
                try {
                    AgentTool.AgentToolResult toolResult = tool.execute(orgId, args);
                    result = toolResult.output();
                    ok = toolResult.ok();
                } catch (IllegalArgumentException e) {
                    result = "Tool error: " + e.getMessage();
                    ok = false;
                    if (iteration >= MAX_ITERATIONS - 1) {
                        finalAnswer = result;
                        break;
                    }
                } catch (Exception e) {
                    log.error("Tool '{}' failed during run {}", toolName, run.getId(), e);
                    result = "Tool error: " + e.getMessage();
                    ok = false;
                }
                if (result.length() > MAX_TOOL_OUTPUT_CHARS) result = result.substring(0, MAX_TOOL_OUTPUT_CHARS);

                steps.add(new Step(toolName, args, result, ok));
                history.add(new AiChatRequest.ChatMessage("user",
                        "<tool_result tool=\"" + toolName + "\">\n" + result + "\n</tool_result>"));
            }
            if (finalAnswer == null) status = "MAX_ITERATIONS";
        } catch (Exception e) {
            log.error("Agent run {} failed", run.getId(), e);
            status = "FAILED";
            error = e.getMessage();
        }

        if (finalAnswer == null) {
            finalAnswer = "MAX_ITERATIONS".equals(status)
                    ? "Stopped after " + MAX_ITERATIONS + " iterations; see steps for partial results."
                    : "(no answer produced)";
        }

        run.setOutput(guardrails.checkHandoffContext(finalAnswer));
        run.setStepsJson(writeSteps(steps));
        run.setStatus(status);
        run.setError(error);
        run.setTokensUsed(totalTokens);
        run.setEstimatedCostMicros(Math.max(0, (long) totalTokens * 2L));
        run.setFinishedAt(Instant.now());
        guardrails.recordUsage(orgId, username, agent, run.getId(), null, usedModel, totalTokens);
        return runRepository.save(run);
    }


    /**
     * Worker execution entry-point for Phase 14 orchestration.
     * Mirrors run() but links the run to a parent orchestration run.
     */
    public AgentRun runWorker(UUID orgId, Agent agent, String input, UUID orchRunId) {
        String username = currentUsername();
        String safeInput = guardrails.checkInput(input);
        guardrails.checkInvocation(orgId, agent, username);
        AiConfiguration config = configRepository.findByOrganizationId(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AI is not configured for this organization"));
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI is disabled in AI Configuration");
        }

        AgentRun run = runRepository.save(new AgentRun(orgId, agent.getId(), agent.getName(), safeInput));
        run.setUsername(username);
        run.setModel(agent.getModel() != null ? agent.getModel() : config.getModel());
        run = runRepository.save(run);
        List<AgentTool> allowed = agent.getAllowedTools() == null || agent.getAllowedTools().isBlank()
                ? List.of() : toolRegistry.resolveWhitelist(agent.getAllowedTools());

        List<Step> steps = new ArrayList<>();
        List<AiChatRequest.ChatMessage> history = new ArrayList<>();
        history.add(new AiChatRequest.ChatMessage("system", buildSystemPrompt(agent, allowed)));
        history.add(new AiChatRequest.ChatMessage("user", input));

        String finalAnswer = null;
        String status = "COMPLETED";
        String error = null;
        int totalTokens = 0;
        String usedModel = run.getModel();

        try {
            for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                AiProvider provider = providerFactory.getProvider(agent.getProvider() != null
                        ? agent.getProvider() : config.getProvider());
                AiChatRequest request = new AiChatRequest(
                        agent.getModel() != null ? agent.getModel() : config.getModel(),
                        history,
                        agent.getTemperature() != null ? agent.getTemperature() : config.getTemperature(),
                        config.getMaxTokens(),
                        config.getApiKey(),
                        config.getBaseUrl());
                AiChatResponse response = provider.chat(request);
                totalTokens += response.tokensUsed() != null ? response.tokensUsed() : 0;
                String content = guardrails.checkOutput(response.content());
                history.add(new AiChatRequest.ChatMessage("assistant", content));

                String toolName = extractTag(content, "tool");
                if (toolName == null) {
                    finalAnswer = content.replaceAll("\uCF80", "").trim();
                    break;
                }

                Map<String, Object> args = parseArgs(extractTag(content, "args"));
                AgentTool next = guardrails.checkTool(allowed, toolName);
                assertUserPermission(next);
                if (next.requiresApproval() || guardrails.requiresApproval(toolName)) {
                    AgentApproval approval = approvals.requestApproval(orgId, run.getId(), orchRunId,
                            agent, next, args, username);
                    run.setOutput("(waiting for human approval: " + toolName + ")");
                    run.setStepsJson(writeSteps(steps));
                    run.setStatus("WAITING_APPROVAL");
                    run.setError("Approval required: " + approval.getId());
                    run.setTokensUsed(totalTokens);
                    run.setFinishedAt(null);
                    guardrails.recordUsage(orgId, username, agent, run.getId(), orchRunId, usedModel, totalTokens);
                    return runRepository.save(run);
                }
                String result;
                try {
                    result = next.execute(orgId, args).output();
                } catch (Exception e) {
                    result = "Tool error: " + e.getMessage();
                }
                if (result.length() > MAX_TOOL_OUTPUT_CHARS) result = result.substring(0, MAX_TOOL_OUTPUT_CHARS);

                steps.add(new Step(toolName, args, result, true));
                history.add(new AiChatRequest.ChatMessage("user",
                        "<tool_result tool=\"" + toolName + "\">\n" + result + "\n</tool_result>"));
            }
            if (finalAnswer == null) status = "MAX_ITERATIONS";
        } catch (Exception e) {
            log.error("Worker run {} failed", run.getId(), e);
            status = "FAILED";
            error = e.getMessage();
        }

        if (finalAnswer == null) {
            finalAnswer = "MAX_ITERATIONS".equals(status)
                    ? "Stopped after " + MAX_ITERATIONS + " iterations; see steps for partial results."
                    : "(no answer produced)";
        }

        run.setOutput(guardrails.checkHandoffContext(finalAnswer));
        run.setStepsJson(writeSteps(steps));
        run.setStatus(status);
        run.setError(error);
        run.setTokensUsed(totalTokens);
        run.setEstimatedCostMicros(Math.max(0, (long) totalTokens * 2L));
        run.setFinishedAt(Instant.now());
        guardrails.recordUsage(orgId, username, agent, run.getId(), orchRunId, usedModel, totalTokens);
        return runRepository.save(run);
    }
/**
     * Continues a WAITING_APPROVAL run after a human approved one tool call
     * (Phase 13 §7). Every guardrail is re-verified before the approved tool
     * executes: tenant, agent state, usage quota, tool whitelist and user permission.
     */
    public AgentRun continueAfterApproval(UUID orgId, Agent agent, AgentRun run,
                                          AgentTool tool, Map<String, Object> approvedArgs,
                                          String decidedBy) {
        return resumeLoop(orgId, agent, run, tool, approvedArgs, decidedBy, null);
    }

    /**
     * Same as {@link #continueAfterApproval} but for a worker run owned by a
     * Phase 14 orchestration, so usage is attributed to the orchestration run.
     */
    public AgentRun resumeWorkerAfterApproval(UUID orgId, Agent agent, AgentRun run,
                                              AgentTool tool, Map<String, Object> approvedArgs,
                                              String decidedBy, UUID orchRunId) {
        return resumeLoop(orgId, agent, run, tool, approvedArgs, decidedBy, orchRunId);
    }

    /** Executes one approved tool then runs the bounded loop to completion. */
    private AgentRun resumeLoop(UUID orgId, Agent agent, AgentRun run,
                                AgentTool firstTool, Map<String, Object> firstArgs,
                                String decidedBy, UUID orchRunId) {
        String username = run.getUsername() != null ? run.getUsername() : currentUsername();
        if (!orgId.equals(run.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent run not found");
        }
        guardrails.checkInvocation(orgId, agent, username);
        AiConfiguration config = configRepository.findByOrganizationId(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AI is not configured for this organization"));
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI is disabled in AI Configuration");
        }
        List<AgentTool> allowed = agent.getAllowedTools() == null || agent.getAllowedTools().isBlank()
                ? List.of() : toolRegistry.resolveWhitelist(agent.getAllowedTools());
        guardrails.checkTool(allowed, firstTool.name());
        assertUserPermission(firstTool);
        List<Step> steps = readSteps(run.getStepsJson());
        int totalTokens = run.getTokensUsed() != null ? run.getTokensUsed() : 0;
        List<AiChatRequest.ChatMessage> history = new ArrayList<>();
        history.add(new AiChatRequest.ChatMessage("system", buildSystemPrompt(agent, allowed)));
        history.add(new AiChatRequest.ChatMessage("user", run.getInput()));
        String toolText;
        boolean toolOk;
        try {
            AgentTool.AgentToolResult toolResult = firstTool.execute(orgId, firstArgs);
            toolText = toolResult.output();
            toolOk = toolResult.ok();
        } catch (Exception e) {
            log.error("Approved tool '{}' failed during resume of run {}", firstTool.name(), run.getId(), e);
            toolText = "Tool error: " + e.getMessage();
            toolOk = false;
        }
        if (toolText.length() > MAX_TOOL_OUTPUT_CHARS) toolText = toolText.substring(0, MAX_TOOL_OUTPUT_CHARS);
        steps.add(new Step(firstTool.name() + " (approved by " + decidedBy + ")", firstArgs, toolText, toolOk));
        history.add(new AiChatRequest.ChatMessage("user",
                "<tool_result tool=\"" + firstTool.name() + "\">\n" + toolText + "\n</tool_result>"));
        String finalAnswer = null;
        String status = "COMPLETED";
        String error = null;
        try {
            for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                AiProvider provider = providerFactory.getProvider(agent.getProvider() != null
                        ? agent.getProvider() : config.getProvider());
                AiChatRequest request = new AiChatRequest(
                        agent.getModel() != null ? agent.getModel() : config.getModel(),
                        history,
                        agent.getTemperature() != null ? agent.getTemperature() : config.getTemperature(),
                        config.getMaxTokens(), config.getApiKey(), config.getBaseUrl());
                AiChatResponse response = provider.chat(request);
                totalTokens += response.tokensUsed() != null ? response.tokensUsed() : 0;
                String content = guardrails.checkOutput(response.content());
                history.add(new AiChatRequest.ChatMessage("assistant", content));
                String toolName = extractTag(content, "tool");
                if (toolName == null) {
                    finalAnswer = content.replaceAll("\uCF80", "").trim();
                    break;
                }
                Map<String, Object> args = parseArgs(extractTag(content, "args"));
                AgentTool next = guardrails.checkTool(allowed, toolName);
                assertUserPermission(next);
                if (next.requiresApproval() || guardrails.requiresApproval(toolName)) {
                    AgentApproval approval = approvals.requestApproval(orgId, run.getId(), orchRunId,
                            agent, next, args, username);
                    run.setOutput("(waiting for human approval: " + toolName + ")");
                    run.setStepsJson(writeSteps(steps));
                    run.setStatus("WAITING_APPROVAL");
                    run.setError("Approval required: " + approval.getId());
                    run.setTokensUsed(totalTokens);
                    run.setFinishedAt(null);
                    guardrails.recordUsage(orgId, username, agent, run.getId(), orchRunId, run.getModel(), totalTokens);
                    return runRepository.save(run);
                }
                String result;
                try {
                    result = next.execute(orgId, args).output();
                } catch (Exception e) {
                    result = "Tool error: " + e.getMessage();
                }
                if (result.length() > MAX_TOOL_OUTPUT_CHARS) result = result.substring(0, MAX_TOOL_OUTPUT_CHARS);
                steps.add(new Step(toolName, args, result, true));
                history.add(new AiChatRequest.ChatMessage("user",
                        "<tool_result tool=\"" + toolName + "\">\n" + result + "\n</tool_result>"));
            }
            if (finalAnswer == null) status = "MAX_ITERATIONS";
        } catch (Exception e) {
            log.error("Agent resume {} failed", run.getId(), e);
            status = "FAILED";
            error = e.getMessage();
        }
        if (finalAnswer == null) {
            finalAnswer = "MAX_ITERATIONS".equals(status)
                    ? "Stopped after " + MAX_ITERATIONS + " iterations; see steps for partial results."
                    : "(no answer produced)";
        }
        run.setOutput(guardrails.checkHandoffContext(finalAnswer));
        run.setStepsJson(writeSteps(steps));
        run.setStatus(status);
        run.setError(error);
        run.setTokensUsed(totalTokens);
        run.setEstimatedCostMicros(Math.max(0, (long) totalTokens * 2L));
        run.setFinishedAt(Instant.now());
        guardrails.recordUsage(orgId, username, agent, run.getId(), orchRunId, run.getModel(), totalTokens);
        return runRepository.save(run);
    }

    public List<AgentTool> tools() {
        return new ArrayList<>(toolRegistry.all());
    }

    public List<AgentRun> runs(UUID orgId, UUID agentId) {
        return agentId == null
                ? runRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId)
                : runRepository.findAllByOrganizationIdAndAgentIdOrderByCreatedAtDesc(orgId, agentId);
    }

    /** Tenant-scoped lookup of one run (used by the Phase 14 orchestrator). */
    public AgentRun getRun(UUID orgId, UUID runId) {
        return runRepository.findById(runId)
                .filter(r -> r.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent run not found"));
    }

    // ---------- Prompt & protocol helpers ----------

    private String currentUsername() {
        UserContext.CurrentUser u = UserContext.get();
        if (u != null && u.username() != null) return u.username();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private void assertUserPermission(AgentTool tool) {
        String required = tool.requiredPermission();
        if (required == null || required.isBlank()) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No user context");
        }
        boolean granted = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("PERMISSION_" + required)
                        || a.getAuthority().equals(required)
                        || a.getAuthority().equals("PLATFORM_ADMIN"));
        if (!granted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User lacks permission for tool '" + tool.name() + "' (needs " + required + ")");
        }
    }

    private List<Step> readSteps(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Step>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }




    private String buildSystemPrompt(Agent agent, List<AgentTool> allowed) {
        StringBuilder sb = new StringBuilder();
        String base = agent.getSystemPrompt() != null ? agent.getSystemPrompt() :
                "You are a helpful AI agent. Use the available tools to accomplish tasks.";
        sb.append(base).append("\n\n");
        sb.append("Available tools: ").append(allowed.isEmpty() ? "none" :
                String.join(", ", allowed.stream().map(AgentTool::name).toList())).append("\n");
        sb.append("\nTo call a tool, use this XML-like format:\n");
        sb.append("<tool>tool_name</tool>\n");
        sb.append("<args key=\"param\">value</args>\n");
        sb.append("\nWhen you are done, respond with your final answer (no tool call).");
        sb.append("\nAfter a successful tool call, you will receive the tool_result and continue.");
        return sb.toString();
    }

    private static String extractTag(String content, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = content.indexOf(open);
        if (start < 0) return null;
        start += open.length();
        int end = content.indexOf(close, start);
        if (end < 0) return null;
        return content.substring(start, end).trim();
    }

    private static Map<String, Object> parseArgs(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        Map<String, Object> map = new LinkedHashMap<>();
        int pos = 0;
        while (pos < raw.length()) {
            int eq = raw.indexOf('=', pos);
            if (eq < 0) break;
            String key = raw.substring(pos, eq).trim();
            pos = eq + 1;
            if (pos >= raw.length()) break;
            char quote = raw.charAt(pos);
            String val;
            if (quote == '"' || quote == '\'') {
                pos++;
                int end = raw.indexOf(quote, pos);
                if (end < 0) { val = raw.substring(pos); pos = raw.length(); }
                else { val = raw.substring(pos, end); pos = end + 1; }
            } else {
                int end = raw.indexOf(' ', pos);
                if (end < 0) { val = raw.substring(pos); pos = raw.length(); }
                else { val = raw.substring(pos, end); pos = end; }
            }
            while (pos < raw.length() && Character.isWhitespace(raw.charAt(pos))) pos++;
            map.put(key, val);
        }
        return map;
    }

    private String writeSteps(List<Step> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public record Step(String tool, Map<String, Object> args, String result, boolean ok) {}
}

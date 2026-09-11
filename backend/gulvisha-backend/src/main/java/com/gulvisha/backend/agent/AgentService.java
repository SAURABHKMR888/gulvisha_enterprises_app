package com.gulvisha.backend.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulvisha.backend.ai.entity.AiConfiguration;
import com.gulvisha.backend.ai.provider.AiChatRequest;
import com.gulvisha.backend.ai.provider.AiProvider;
import com.gulvisha.backend.ai.provider.AiProviderFactory;
import com.gulvisha.backend.ai.repository.AiConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent runtime (Phase 13): runs an {@link Agent} through a bounded
 * tool-calling loop using the tenant's configured AI provider.
 * Every run is persisted in {@link AgentRun} for audit.
 */
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
    private final ObjectMapper objectMapper;

    public AgentService(AgentRepository agentRepository,
                        AgentRunRepository runRepository,
                        AgentToolRegistry toolRegistry,
                        AiConfigurationRepository configRepository,
                        AiProviderFactory providerFactory,
                        ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.runRepository = runRepository;
        this.toolRegistry = toolRegistry;
        this.configRepository = configRepository;
        this.providerFactory = providerFactory;
        this.objectMapper = objectMapper;
    }

    // ---------- CRUD ----------

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
        if (!agent.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agent is disabled");
        }
        AiConfiguration config = configRepository.findByOrganizationId(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AI is not configured for this organization"));
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI is disabled in AI → Configuration");
        }

        AgentRun run = runRepository.save(new AgentRun(orgId, agent.getId(), agent.getName(), input));
        List<AgentTool> allowed = agent.getAllowedTools() == null || agent.getAllowedTools().isBlank()
                ? List.of() : toolRegistry.resolveWhitelist(agent.getAllowedTools());

        List<Step> steps = new ArrayList<>();
        List<AiChatRequest.ChatMessage> history = new ArrayList<>();
        history.add(new AiChatRequest.ChatMessage("system", buildSystemPrompt(agent, allowed)));
        history.add(new AiChatRequest.ChatMessage("user", input));

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
                        config.getMaxTokens(),
                        config.getApiKey(),
                        config.getBaseUrl());
                String content = provider.chat(request).content();
                history.add(new AiChatRequest.ChatMessage("assistant", content));

                String toolName = extractTag(content, "tool");
                if (toolName == null) {
                    finalAnswer = content.replaceAll("</think>", "").trim();
                    break;
                }

                Map<String, Object> args = parseArgs(extractTag(content, "args"));
                AgentTool tool = allowed.stream()
                        .filter(t -> t.name().equals(toolName))
                        .findFirst()
                        .orElse(null);

                String result;
                boolean ok = tool != null;
                if (tool == null) {
                    result = "ERROR: tool '" + toolName + "' is not allowed for this agent. Allowed tools: "
                            + allowed.stream().map(AgentTool::name).toList();
                } else {
                    try {
                        AgentTool.AgentToolResult r = tool.execute(orgId, args);
                        ok = r.ok();
                        result = truncate(r.output());
                    } catch (Exception e) {
                        result = "Tool error: " + e.getMessage();
                    }
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

        run.setOutput(finalAnswer);
        run.setStepsJson(writeSteps(steps));
        run.setStatus(status);
        run.setError(error);
        run.setFinishedAt(Instant.now());
        return runRepository.save(run);
    }

    public List<AgentRun> runs(UUID orgId, UUID agentId) {
        return agentId == null
                ? runRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId)
                : runRepository.findAllByOrganizationIdAndAgentIdOrderByCreatedAtDesc(orgId, agentId);
    }

    // ---------- Prompt & protocol helpers ----------

    private String buildSystemPrompt(Agent agent, List<AgentTool> allowed) {
        StringBuilder sb = new StringBuilder(agent.getSystemPrompt() == null ? "" : agent.getSystemPrompt());
        sb.append("\n\nYou may use tools. To call one, reply with exactly one line:\n");
        sb.append("<tool>tool_name</tool><args>{\"key\": \"value\"}</args>\n");
        sb.append("After each call you receive a <tool_result>. When you have enough information, ");
        sb.append("reply with the final answer and NO tool tags.\n");
        if (allowed.isEmpty()) {
            sb.append("No tools are available: answer directly from your instructions.\n");
        } else {
            sb.append("Available tools:\n");
            for (AgentTool t : allowed) {
                sb.append("- ").append(t.name()).append(": ").append(t.description())
                  .append(" Args: ").append(t.argumentSpec()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String extractTag(String content, String tag) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<" + tag + ">([\\s\\S]*?)</" + tag + ">").matcher(content);
        return m.find() ? m.group(1).trim() : null;
    }

    private Map<String, Object> parseArgs(String json) throws JsonProcessingException {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > MAX_TOOL_OUTPUT_CHARS ? s.substring(0, MAX_TOOL_OUTPUT_CHARS) + "…" : s;
    }

    private String writeSteps(List<Step> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            return "[]";
        }
    }

    public record Step(String tool, Object args, String result, boolean ok) {}
}

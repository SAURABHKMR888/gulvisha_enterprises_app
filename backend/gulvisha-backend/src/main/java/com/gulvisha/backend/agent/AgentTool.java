package com.gulvisha.backend.agent;

import java.util.Map;
import java.util.UUID;

/**
 * A tool an {@link Agent} may invoke during a run (Phase 13).
 * Implementations must be tenant-safe: they receive the organizationId of the
 * agent's tenant and MUST only touch data belonging to that tenant.
 */
public interface AgentTool {

    /** Unique tool name the LLM refers to, e.g. "search_knowledge_base". */
    String name();

    /** One-line description injected into the agent system prompt. */
    String description();

    /**
     * JSON-ish argument spec injected into the system prompt,
     * e.g. "{ \"query\": \"text to search for\" }".
     */
    String argumentSpec();

    /**
     * Platform permission required to invoke this tool, e.g. "lead:create".
     * The orchestrator and guardrail layer deny execution when the
     * invoking user lacks it (Phase 13 §4 user check, Phase 14 §6).
     */
    default String requiredPermission() { return ""; }

    /** True when invoking this tool must pause for human approval first. */
    default boolean requiresApproval() { return false; }

    /**
     * Execute the tool. Args are the parsed JSON object the model produced.
     * Implementations should throw IllegalArgumentException on bad input —
     * the message is fed back to the model for self-correction.
     */
    AgentToolResult execute(UUID organizationId, Map<String, Object> args);

    /** Result of one tool invocation. */
    record AgentToolResult(boolean ok, String output) {
        public static AgentToolResult success(String output) { return new AgentToolResult(true, output); }
        public static AgentToolResult error(String message) { return new AgentToolResult(false, message); }
    }
}

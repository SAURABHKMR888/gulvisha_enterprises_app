package com.gulvisha.backend.agent;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of all {@link AgentTool} implementations available to agents.
 * An agent may only call tools listed in its allowedTools whitelist.
 */
@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> tools = new HashMap<>();

    public AgentToolRegistry(List<AgentTool> implementations) {
        for (AgentTool tool : implementations) {
            tools.put(tool.name(), tool);
        }
    }

    public AgentTool get(String name) {
        return tools.get(name);
    }

    public Collection<AgentTool> all() {
        return tools.values();
    }

    /** Resolves an agent's comma-separated whitelist to tool instances (unknown names ignored). */
    public List<AgentTool> resolveWhitelist(String allowedTools) {
        Set<String> names = new LinkedHashSet<>();
        for (String n : allowedTools.split(",")) {
            String trimmed = n.trim();
            if (!trimmed.isEmpty()) names.add(trimmed);
        }
        return names.stream().map(tools::get).filter(java.util.Objects::nonNull).toList();
    }

    public Set<String> availableToolNames() {
        return tools.keySet();
    }
}

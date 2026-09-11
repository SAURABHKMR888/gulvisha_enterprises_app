package com.gulvisha.backend.agent;

import com.gulvisha.backend.security.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for tenant AI agents (Phase 13).
 * Access is already gated by SecurityConfig: requires the "ai:agent:execute" permission.
 */
@RestController
@RequestMapping("/api/ai/agents")
public class AgentController {

    private final AgentService agentService;
    private final AgentToolRegistry toolRegistry;

    public AgentController(AgentService agentService, AgentToolRegistry toolRegistry) {
        this.agentService = agentService;
        this.toolRegistry = toolRegistry;
    }

    @GetMapping
    public List<Agent> list() {
        return agentService.list(UserContext.getOrganizationId());
    }

    @GetMapping("/{id}")
    public Agent get(@PathVariable UUID id) {
        return agentService.get(UserContext.getOrganizationId(), id);
    }

    @PostMapping
    public Agent create(@RequestBody Agent agent) {
        return agentService.create(UserContext.getOrganizationId(), agent);
    }

    @PutMapping("/{id}")
    public Agent update(@PathVariable UUID id, @RequestBody Agent agent) {
        return agentService.update(UserContext.getOrganizationId(), id, agent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        agentService.delete(UserContext.getOrganizationId(), id);
        return ResponseEntity.noContent().build();
    }

    /** Runs the agent on an input and returns the persisted run (answer + steps). */
    @PostMapping("/{id}/run")
    public AgentRun run(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Agent agent = agentService.get(UserContext.getOrganizationId(), id);
        String input = body.getOrDefault("input", "");
        return agentService.run(UserContext.getOrganizationId(), agent, input);
    }

    /** Run history: all runs for the tenant, optionally filtered by agent. */
    @GetMapping("/runs")
    public List<AgentRun> runs(@RequestParam(required = false) UUID agentId) {
        return agentService.runs(UserContext.getOrganizationId(), agentId);
    }

    /** Tool catalog so the UI can show what an agent may be allowed to call. */
    @GetMapping("/tools")
    public List<Map<String, String>> tools() {
        return toolRegistry.all().stream()
                .map(t -> Map.of("name", t.name(), "description", t.description(), "args", t.argumentSpec()))
                .toList();
    }
}

package com.gulvisha.backend.agent.orchestration;

import com.gulvisha.backend.security.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for multi-agent orchestrations (Phase 14).
 * Tenant isolation via UserContext; auth via SecurityConfig /api/ai/**.
 */
@RestController
@RequestMapping("/api/ai/orchestrations")
public class OrchestrationController {

    private final OrchestrationService orchestrationService;

    public OrchestrationController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @GetMapping
    public List<AgentOrchestration> list() {
        return orchestrationService.list(UserContext.getOrganizationId());
    }

    @GetMapping("/{id}")
    public AgentOrchestration get(@PathVariable UUID id) {
        return orchestrationService.get(UserContext.getOrganizationId(), id);
    }

    @PostMapping
    public AgentOrchestration create(@RequestBody AgentOrchestration o) {
        return orchestrationService.create(UserContext.getOrganizationId(), withOrg(o));
    }

    @PutMapping("/{id}")
    public AgentOrchestration update(@PathVariable UUID id, @RequestBody AgentOrchestration o) {
        return orchestrationService.update(UserContext.getOrganizationId(), id, withOrg(o));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orchestrationService.delete(UserContext.getOrganizationId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/run")
    public OrchestrationRun run(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return orchestrationService.run(UserContext.getOrganizationId(), id,
                body.getOrDefault("input", ""));
    }

    @GetMapping("/runs")
    public List<OrchestrationRun> runs(@RequestParam(required = false) UUID orchestrationId) {
        return orchestrationService.runs(UserContext.getOrganizationId(), orchestrationId);
    }

    @GetMapping("/runs/{runId}")
    public OrchestrationRun runDetail(@PathVariable UUID runId) {
        return orchestrationService.getRun(UserContext.getOrganizationId(), runId);
    }

    private AgentOrchestration withOrg(AgentOrchestration o) {
        try {
            java.lang.reflect.Field f = AgentOrchestration.class.getDeclaredField("organizationId");
            f.setAccessible(true);
            f.set(o, UserContext.getOrganizationId());
        } catch (Exception ignored) {
        }
        return o;
    }
}

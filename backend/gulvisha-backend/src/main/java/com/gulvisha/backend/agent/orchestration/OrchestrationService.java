package com.gulvisha.backend.agent.orchestration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulvisha.backend.agent.Agent;
import com.gulvisha.backend.agent.AgentRun;
import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.agent.AgentService;
import com.gulvisha.backend.agent.guardrail.AgentApproval;
import com.gulvisha.backend.agent.guardrail.AgentApprovalService;
import com.gulvisha.backend.agent.guardrail.AgentGuardrailService;
import com.gulvisha.backend.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Generic multi-agent orchestrator (Phase 14). Reuses the Phase 13
 * AgentService run loop + guardrail/approval/usage layer for EVERY
 * worker invocation.
 */
@Service
public class OrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationService.class);

    /** Hard ceiling on the parallel worker pool (§4 "where safe" bound). */
    private static final int MAX_PARALLEL_STEPS = 5;

    private final AgentOrchestrationRepository orchestrationRepository;
    private final OrchestrationRunRepository runRepository;
    private final AgentService agentService;
    private final AgentGuardrailService guardrails;
    private final AgentApprovalService approvals;
    private final ObjectMapper objectMapper;

    public OrchestrationService(AgentOrchestrationRepository orchestrationRepository,
                                OrchestrationRunRepository runRepository,
                                AgentService agentService,
                                AgentGuardrailService guardrails,
                                AgentApprovalService approvals,
                                ObjectMapper objectMapper) {
        this.orchestrationRepository = orchestrationRepository;
        this.runRepository = runRepository;
        this.agentService = agentService;
        this.guardrails = guardrails;
        this.approvals = approvals;
        this.objectMapper = objectMapper;
    }

    public List<AgentOrchestration> list(UUID orgId) {
        return orchestrationRepository.findAllByOrganizationIdOrderByNameAsc(orgId);
    }

    public AgentOrchestration get(UUID orgId, UUID id) {
        return orchestrationRepository.findByOrganizationIdAndId(orgId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orchestration not found"));
    }

    public AgentOrchestration create(UUID orgId, AgentOrchestration o) {
        validate(o, orgId);
        return orchestrationRepository.save(o);
    }

    public AgentOrchestration update(UUID orgId, UUID id, AgentOrchestration changes) {
        AgentOrchestration o = get(orgId, id);
        o.setName(changes.getName());
        o.setDescription(changes.getDescription());
        o.setSupervisorAgentId(changes.getSupervisorAgentId());
        o.setStepsJson(changes.getStepsJson());
        o.setExecutionMode(changes.getExecutionMode());
        o.setMaxSteps(changes.getMaxSteps());
        o.setMaxRetriesPerStep(changes.getMaxRetriesPerStep());
        o.setDefaultTimeoutSeconds(changes.getDefaultTimeoutSeconds());
        o.setEnabled(changes.isEnabled());
        validate(o, orgId);
        return orchestrationRepository.save(o);
    }

    public void delete(UUID orgId, UUID id) {
        orchestrationRepository.delete(get(orgId, id));
    }


    private void validate(AgentOrchestration o, UUID orgId) {
        List<StepSpec> steps = parseSteps(o.getStepsJson());
        if (steps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least one step");
        }
        if (steps.size() > Math.max(1, o.getMaxSteps())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many steps for maxSteps");
        }
        if (o.getSupervisorAgentId() != null) {
            agentService.get(orgId, o.getSupervisorAgentId());
        }
        for (StepSpec s : steps) {
            UUID agentId;
            try {
                agentId = UUID.fromString(s.agentId());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid agentId in steps");
            }
            Agent agent = agentService.get(orgId, agentId);
            if (o.getSupervisorAgentId() != null && o.getSupervisorAgentId().equals(agent.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Supervisor cannot also be a worker (prevents recursion)");
            }
        }
    }

    List<StepSpec> parseSteps(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            List<StepSpec> out = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                Object agentId = m.get("agentId");
                if (agentId == null) continue;
                out.add(new StepSpec(agentId.toString(),
                        str(m.get("mode"), "SEQUENTIAL"),
                        str(m.get("condition"), null),
                        num(m.get("maxRetries"), 1),
                        num(m.get("timeoutSeconds"), 180)));
            }
            return out;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stepsJson is not valid JSON");
        }
    }

    private static String str(Object v, String dflt) {
        return v == null ? dflt : v.toString();
    }

    private static int num(Object v, int dflt) {
        if (v instanceof Number n) return n.intValue();
        try {
            return v == null ? dflt : Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public OrchestrationRun run(UUID orgId, UUID orchestrationId, String input) {
        AgentOrchestration orch = get(orgId, orchestrationId);
        if (!orch.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Orchestration is disabled");
        }
        String username = currentUsername();
        String safeInput = guardrails.checkInput(input);
        List<StepSpec> steps = parseSteps(orch.getStepsJson());
        OrchestrationRun run = runRepository.save(
                new OrchestrationRun(orgId, orch.getId(), orch.getName(), username, safeInput));
        List<Map<String, Object>> records = new ArrayList<>();
        String context = safeInput;
        String status = "COMPLETED";
        String error = null;
        int totalTokens = 0;
        try {
            if ("PARALLEL_SAFE".equalsIgnoreCase(orch.getExecutionMode())) {
                Map<Integer, Map<String, Object>> byIndex = runParallelSafely(orgId, username, steps,
                        context, run.getId(), orch.getDefaultTimeoutSeconds(),
                        orch.getMaxRetriesPerStep(), Math.max(1, orch.getMaxSteps()));
                for (int i = 0; i < steps.size(); i++) {
                    Map<String, Object> rec = byIndex.get(i);
                    if (rec == null) {
                        rec = record(i, steps.get(i), "FAILED", 0, 0, "step not executed", null);
                    }
                    records.add(rec);
                    totalTokens += tokensOf(rec);
                    String recStatus = (String) rec.get("status");
                    if ("FAILED".equals(recStatus) && !"FAILED".equals(status)) {
                        status = "FAILED";
                        error = (String) rec.get("error");
                    } else if ("WAITING_APPROVAL".equals(recStatus) && !"FAILED".equals(status)) {
                        status = "WAITING_APPROVAL";
                        error = (String) rec.get("error");
                    }
                }
                if ("COMPLETED".equals(status) && !records.isEmpty()) {
                    context = guardrails.checkHandoffContext(
                            (String) records.get(records.size() - 1).get("output"));
                }
            } else {
            for (int i = 0; i < steps.size(); i++) {
                StepSpec step = steps.get(i);
                if ("CONDITIONAL".equalsIgnoreCase(orch.getExecutionMode())
                        || "CONDITIONAL".equalsIgnoreCase(step.mode())) {
                    if (!conditionMatches(step.condition(), context)) {
                        records.add(record(i, step, "SKIPPED", 0, 0, "condition not met", null));
                        continue;
                    }
                }
                Map<String, Object> rec = runStepWithRetry(orgId, username, i, step, context,
                        run.getId(), orch.getDefaultTimeoutSeconds(), orch.getMaxRetriesPerStep());
                records.add(rec);
                totalTokens += tokensOf(rec);
                String recStatus = (String) rec.get("status");
                if ("WAITING_APPROVAL".equals(recStatus)) {
                    status = "WAITING_APPROVAL";
                    error = (String) rec.get("error");
                    break;
                }
                if ("FAILED".equals(recStatus)) {
                    status = "FAILED";
                    error = (String) rec.get("error");
                    break;
                }
                context = guardrails.checkHandoffContext((String) rec.get("output"));
            }
            }
        } catch (Exception e) {
            log.error("Orchestration run {} failed", run.getId(), e);
            status = "FAILED";
            error = e.getMessage();
        }
        run.setOutput(context);
        run.setStepsJson(writeJson(records));
        run.setStatus(status);
        run.setError(error);
        run.setTotalTokens(totalTokens);
        run.setEstimatedCostMicros(Math.max(0, (long) totalTokens * 2L));
        run.setFinishedAt("COMPLETED".equals(status) || "FAILED".equals(status) ? Instant.now() : null);
        return runRepository.save(run);
    }

    /**
     * §4 bounded parallel execution. Independent steps run concurrently on a
     * pool capped by maxSteps and MAX_PARALLEL_STEPS. Each step is still
     * independently guardrailed (tenant, user, agent, quota, tools, actions),
     * timeout-bounded and retry-bounded by runStepWithRetry. Each receives the
     * orchestration input as its context rather than a sibling branch's output,
     * so no uncontrolled cross-agent context sharing can occur.
     */
    private Map<Integer, Map<String, Object>> runParallelSafely(UUID orgId, String username,
                                                                List<StepSpec> steps, String context,
                                                                UUID orchRunId, int defaultTimeout,
                                                                int maxRetriesPerStep, int maxParallel) {
        Map<Integer, Map<String, Object>> results = new LinkedHashMap<>();
        int poolSize = Math.min(Math.min(Math.max(1, maxParallel), Math.max(1, steps.size())),
                MAX_PARALLEL_STEPS);
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        List<Integer> indexes = new ArrayList<>();
        List<Future<Map<String, Object>>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < steps.size(); i++) {
                final int index = i;
                final StepSpec step = steps.get(i);
                indexes.add(index);
                futures.add(pool.submit(new Callable<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> call() {
                        return runStepWithRetry(orgId, username, index, step, context,
                                orchRunId, defaultTimeout, maxRetriesPerStep);
                    }
                }));
            }
            for (int f = 0; f < futures.size(); f++) {
                int index = indexes.get(f);
                try {
                    results.put(index, futures.get(f).get());
                } catch (Exception e) {
                    log.warn("Parallel step {} of orchestration run {} failed", index, orchRunId, e);
                    results.put(index, record(index, steps.get(index), "FAILED", 0, 0,
                            "parallel step failed: " + e.getMessage(), null));
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return results;
    }

    private Map<String, Object> runStepWithRetry(UUID orgId, String username, int index, StepSpec step,
                                                 String context, UUID orchRunId,
                                                 int defaultTimeout, int orchMaxRetries) {
        int maxRetries = Math.min(Math.max(0, step.maxRetries()), Math.max(0, orchMaxRetries));
        maxRetries = Math.min(maxRetries, 3);
        int timeoutSeconds = Math.min(Math.max(10, step.timeoutSeconds() > 0 ? step.timeoutSeconds() : defaultTimeout), 600);
        Exception lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            Instant start = Instant.now();
            try {
                UUID agentId = UUID.fromString(step.agentId());
                Agent agent = agentService.get(orgId, agentId);
                guardrails.checkInvocation(orgId, agent, username);
                AgentRun workerRun = runWithTimeout(orgId, agent,
                        guardrails.checkHandoffContext(context), orchRunId, timeoutSeconds, username);
                long durationMs = Duration.between(start, Instant.now()).toMillis();
                int tokens = workerRun.getTokensUsed() != null ? workerRun.getTokensUsed() : 0;
                if ("WAITING_APPROVAL".equals(workerRun.getStatus())) {
                    return record(index, step, "WAITING_APPROVAL", attempt, durationMs,
                            workerRun.getError(), workerRun);
                }
                if ("FAILED".equals(workerRun.getStatus())) {
                    lastError = new IllegalStateException(workerRun.getError());
                    continue;
                }
                return record(index, step, "COMPLETED", attempt, durationMs,
                        workerRun.getOutput(), workerRun);
            } catch (Exception e) {
                lastError = e;
                if (e instanceof ResponseStatusException rse
                        && (rse.getStatusCode() == HttpStatus.FORBIDDEN
                            || rse.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS
                            || rse.getStatusCode() == HttpStatus.NOT_FOUND)) {
                    break;
                }
            }
        }
        String failure = lastError == null ? null : lastError.getMessage();
        return record(index, step, "FAILED", maxRetries, 0,
                failure == null || failure.isBlank() ? "unknown error" : failure, null);
    }

    /**
     * Resumes a worker run that paused for human approval, then lets the
     * orchestration continue from that step. The approved tool executes only
     * after AgentService re-verifies tenant, agent state, usage quota, tool
     * whitelist and user permission.
     */
    private Map<String, Object> resumePausedWorker(UUID orgId, String username, int index, StepSpec step,
                                                   UUID workerRunId, AgentApproval approval,
                                                   UUID orchRunId) {
        AgentRun workerRun = agentService.getRun(orgId, workerRunId);
        Agent agent = agentService.get(orgId, workerRun.getAgentId());
        guardrails.checkInvocation(orgId, agent, username);
        AgentTool tool = agentService.tools().stream()
                .filter(t -> t.name().equals(approval.getToolName()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tool no longer available"));
        Instant start = Instant.now();
        AgentRun resumed = agentService.resumeWorkerAfterApproval(orgId, agent, workerRun, tool,
                parseApprovalArgs(approval.getArgsJson()), approval.getDecidedBy(), orchRunId);
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        if ("WAITING_APPROVAL".equals(resumed.getStatus())) {
            return record(index, step, "WAITING_APPROVAL", 0, durationMs, resumed.getError(), resumed);
        }
        if (!"COMPLETED".equals(resumed.getStatus())) {
            return record(index, step, "FAILED", 0, durationMs, resumed.getError(), resumed);
        }
        return record(index, step, "COMPLETED", 0, durationMs, resumed.getOutput(), resumed);
    }

    private Map<String, Object> parseApprovalArgs(String argsJson) {
        if (argsJson == null || argsJson.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(argsJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stored approval args are invalid");
        }
    }

    private AgentRun runWithTimeout(UUID orgId, Agent agent, String input, UUID orchRunId,
                                    int timeoutSeconds, String username) {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        UserContext.CurrentUser callerContext = UserContext.get();
        org.springframework.security.core.context.SecurityContext securityContext =
                org.springframework.security.core.context.SecurityContextHolder.getContext();
        Callable<AgentRun> task = () -> {
            UserContext.CurrentUser ctx = callerContext != null ? callerContext
                    : new UserContext.CurrentUser(username, orgId, null, null);
            UserContext.set(ctx);
            org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
            try {
                return agentService.runWorker(orgId, agent, input, orchRunId);
            } finally {
                UserContext.clear();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        };
        Future<AgentRun> future = pool.submit(task);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                    "Worker '" + agent.getName() + "' timed out after " + timeoutSeconds + "s");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ResponseStatusException rse) throw rse;
            if (cause instanceof RuntimeException re) throw re;
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Worker failed: " + cause.getMessage());
        } finally {
            pool.shutdownNow();
        }
    }

    private Map<String, Object> record(int index, StepSpec step, String status, int attempts,
                                       long durationMs, String output, AgentRun workerRun) {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("index", index);
        rec.put("agentId", step.agentId());
        rec.put("mode", step.mode());
        rec.put("status", status);
        rec.put("attempts", attempts);
        rec.put("durationMs", durationMs);
        rec.put("output", output != null ? output : "");
        rec.put("tokens", workerRun != null && workerRun.getTokensUsed() != null ? workerRun.getTokensUsed() : 0);
        rec.put("agentRunId", workerRun != null ? workerRun.getId() : null);
        rec.put("error", "COMPLETED".equals(status) ? null : output);
        return rec;
    }

    private int tokensOf(Map<String, Object> rec) {
        Object t = rec.get("tokens");
        return t instanceof Number n ? n.intValue() : 0;
    }

    boolean conditionMatches(String condition, String context) {
        if (condition == null || condition.isBlank()) return true;
        String c = condition.trim();
        if (c.toLowerCase().startsWith("output contains ")) {
            String needle = c.substring("output contains ".length()).trim();
            return context != null && context.toLowerCase().contains(needle.toLowerCase());
        }
        return true;
    }

    public List<OrchestrationRun> runs(UUID orgId, UUID orchestrationId) {
        return orchestrationId == null
                ? runRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId)
                : runRepository.findAllByOrganizationIdAndOrchestrationIdOrderByCreatedAtDesc(orgId, orchestrationId);
    }

    public OrchestrationRun getRun(UUID orgId, UUID runId) {
        return runRepository.findById(runId)
                .filter(r -> r.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
    }

    /** Persists a REJECTED orchestration run after a human rejects a step (Phase 14 §7). */
    public OrchestrationRun markRejectedAfterDecision(UUID orgId, UUID orchestrationRunId,
                                                      String decidedBy, String toolName) {
        OrchestrationRun run = getRun(orgId, orchestrationRunId);
        run.setStatus("REJECTED");
        run.setError("Rejected by " + decidedBy + ": tool " + toolName);
        run.setFinishedAt(Instant.now());
        return runRepository.save(run);
    }

    public OrchestrationRun resumeAfterApproval(UUID orgId, UUID approvalId) {
        AgentApproval approval = approvals.list(orgId, "APPROVED").stream()
                .filter(a -> a.getId().equals(approvalId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Approval is not in APPROVED state"));
        if (approval.getOrchestrationRunId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Single-agent approvals resume via the agent endpoint");
        }
        OrchestrationRun run = getRun(orgId, approval.getOrchestrationRunId());
        if (!"WAITING_APPROVAL".equals(run.getStatus())) return run;
        AgentOrchestration orch = get(orgId, run.getOrchestrationId());
        List<StepSpec> steps = parseSteps(orch.getStepsJson());
        List<Map<String, Object>> records = readRecords(run.getStepsJson());
        String context = run.getInput();
        int totalTokens = run.getTotalTokens();
        int resumeFrom = 0;
        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> rec = records.get(i);
            if ("WAITING_APPROVAL".equals(rec.get("status"))) {
                resumeFrom = i;
                break;
            }
            if ("COMPLETED".equals(rec.get("status"))) {
                context = guardrails.checkHandoffContext(String.valueOf(rec.get("output")));
                resumeFrom = i + 1;
            }
        }
        String status = "COMPLETED";
        String error = null;
        for (int i = resumeFrom; i < steps.size(); i++) {
            StepSpec step = steps.get(i);
            Map<String, Object> rec;
            if (i == resumeFrom && i < records.size()
                    && "WAITING_APPROVAL".equals(records.get(i).get("status"))) {
                Object workerRunId = records.get(i).get("agentRunId");
                rec = workerRunId == null
                        ? runStepWithRetry(orgId, run.getUsername(), i, step, context,
                                run.getId(), orch.getDefaultTimeoutSeconds(), orch.getMaxRetriesPerStep())
                        : resumePausedWorker(orgId, run.getUsername(), i, step,
                                UUID.fromString(workerRunId.toString()), approval, run.getId());
            } else {
                rec = runStepWithRetry(orgId, run.getUsername(), i, step, context,
                        run.getId(), orch.getDefaultTimeoutSeconds(), orch.getMaxRetriesPerStep());
            }
            if (i < records.size()) records.set(i, rec);
            else records.add(rec);
            totalTokens += tokensOf(rec);
            if ("WAITING_APPROVAL".equals(rec.get("status"))) {
                status = "WAITING_APPROVAL";
                error = (String) rec.get("error");
                break;
            }
            if ("FAILED".equals(rec.get("status"))) {
                status = "FAILED";
                error = (String) rec.get("error");
                break;
            }
            context = guardrails.checkHandoffContext((String) rec.get("output"));
        }
        run.setOutput(context);
        run.setStepsJson(writeJson(records));
        run.setStatus(status);
        run.setError(error);
        run.setTotalTokens(totalTokens);
        run.setEstimatedCostMicros(Math.max(0, (long) totalTokens * 2L));
        run.setFinishedAt("COMPLETED".equals(status) || "FAILED".equals(status) ? Instant.now() : null);
        return runRepository.save(run);
    }

    private List<Map<String, Object>> readRecords(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String currentUsername() {
        UserContext.CurrentUser u = UserContext.get();
        return u == null ? null : u.username();
    }

    public record StepSpec(String agentId, String mode, String condition,
                           int maxRetries, int timeoutSeconds) {}
}

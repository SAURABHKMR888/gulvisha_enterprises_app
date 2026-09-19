package com.gulvisha.backend.agent.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulvisha.backend.agent.Agent;
import com.gulvisha.backend.agent.AgentRun;
import com.gulvisha.backend.agent.AgentService;
import com.gulvisha.backend.agent.guardrail.AgentApproval;
import com.gulvisha.backend.agent.guardrail.AgentApprovalService;
import com.gulvisha.backend.agent.guardrail.AgentGuardrailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Phase 14 §11 - orchestration behaviour tests.
 *
 * Pure unit tests: the Phase 13 dependencies (AgentService and the
 * guardrail/approval layer) are mocked so we can assert exactly how the
 * orchestrator sequences workers, hands off context, bounds retries, pauses for
 * approval, and never bypasses a guardrail.
 */
class OrchestrationServiceTest {

    private AgentOrchestrationRepository orchestrationRepository;
    private OrchestrationRunRepository runRepository;
    private AgentService agentService;
    private AgentGuardrailService guardrails;
    private AgentApprovalService approvals;

    private OrchestrationService service;

    private UUID orgId;
    private UUID otherOrgId;
    private UUID workerAId;
    private UUID workerBId;
    private UUID supervisorId;
    private Agent workerA;
    private Agent workerB;
    private AgentOrchestration orch;

    @BeforeEach
    void setUp() {
        orchestrationRepository = mock(AgentOrchestrationRepository.class);
        runRepository = mock(OrchestrationRunRepository.class);
        agentService = mock(AgentService.class);
        guardrails = mock(AgentGuardrailService.class);
        approvals = mock(AgentApprovalService.class);

        service = new OrchestrationService(orchestrationRepository, runRepository,
                agentService, guardrails, approvals, new ObjectMapper());

        orgId = UUID.randomUUID();
        otherOrgId = UUID.randomUUID();
        workerAId = UUID.randomUUID();
        workerBId = UUID.randomUUID();
        supervisorId = UUID.randomUUID();

        workerA = new Agent(orgId, "Qualifier", null, "qualify leads", null);
        workerB = new Agent(orgId, "Knowledge", null, "answer questions", null);

        orch = new AgentOrchestration(orgId, "Lead flow");
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 1, 60),
                step(workerBId, "SEQUENTIAL", null, 1, 60)));

        willAnswer(inv -> inv.getArgument(0)).given(guardrails).checkInput(anyString());
        willAnswer(inv -> inv.getArgument(0)).given(guardrails)
                .checkHandoffContext(nullable(String.class));
        willAnswer(inv -> inv.getArgument(0)).given(runRepository).save(any());
        willAnswer(inv -> inv.getArgument(0)).given(orchestrationRepository).save(any());

        given(orchestrationRepository.findByOrganizationIdAndId(eq(orgId), any(UUID.class)))
                .willReturn(Optional.of(orch));
        given(agentService.get(eq(orgId), eq(workerAId))).willReturn(workerA);
        given(agentService.get(eq(orgId), eq(workerBId))).willReturn(workerB);
    }

    // ---------- helpers ----------

    private static String step(UUID agentId, String mode, String condition,
                               int maxRetries, int timeoutSeconds) {
        return """
                {"agentId":"%s","mode":"%s","condition":%s,"maxRetries":%d,"timeoutSeconds":%d}
                """.formatted(agentId, mode,
                condition == null ? "null" : "\"" + condition + "\"",
                maxRetries, timeoutSeconds).trim();
    }

    private static String stepsJson(String... steps) {
        return "[" + String.join(",", steps) + "]";
    }

    private AgentRun workerRun(String status, String output, int tokens) {
        AgentRun run = new AgentRun(orgId, UUID.randomUUID(), "worker", "input");
        run.setStatus(status);
        run.setOutput(output);
        // AgentService sets `error` for every non-successful worker run.
        if (!"COMPLETED".equals(status)) run.setError(output);
        run.setTokensUsed(tokens);
        return run;
    }

    private OrchestrationRun run() {
        return service.run(orgId, UUID.randomUUID(), "find new leads");
    }

    // ---------- §4 sequential execution + §5 context handoff + §9 usage ----------

    @Test
    void sequentialStepsRunInOrderHandOffContextAndAggregateUsage() {
        given(agentService.runWorker(eq(orgId), eq(workerA), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "lead-42 qualified", 10));
        given(agentService.runWorker(eq(orgId), eq(workerB), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "final summary", 5));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getOutput()).isEqualTo("final summary");
        assertThat(result.getTotalTokens()).isEqualTo(15);
        assertThat(result.getEstimatedCostMicros()).isEqualTo(30L);
        assertThat(result.getFinishedAt()).isNotNull();
        assertThat(result.getStepsJson()).contains("COMPLETED");

        ArgumentCaptor<String> handoff = ArgumentCaptor.forClass(String.class);
        verify(agentService, times(2)).runWorker(eq(orgId), any(), handoff.capture(), any());
        // worker B receives exactly what worker A produced
        assertThat(handoff.getAllValues().get(0)).isEqualTo("find new leads");
        assertThat(handoff.getAllValues().get(1)).isEqualTo("lead-42 qualified");
    }

    @Test
    void everyHandoffContextPassesThroughTheGuardrail() {
        given(agentService.runWorker(eq(orgId), any(), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "step output", 1));

        run();

        // 1x inbound context per step (2 steps) + 1x per produced output handed on (2 outputs)
        verify(guardrails, times(4)).checkHandoffContext(nullable(String.class));
    }

    // ---------- §4 conditional execution ----------

    @Test
    void conditionalStepIsSkippedWhenItsConditionDoesNotMatch() {
        orch.setExecutionMode("CONDITIONAL");
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 0, 60),
                step(workerBId, "SEQUENTIAL", "output contains APPROVED", 0, 60)));
        given(agentService.runWorker(eq(orgId), eq(workerA), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "lead is cold", 3));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getStepsJson()).contains("SKIPPED");
        verify(agentService, never()).runWorker(eq(orgId), eq(workerB), anyString(), any());
    }

    @Test
    void conditionalStepRunsWhenItsConditionMatches() {
        orch.setExecutionMode("CONDITIONAL");
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 0, 60),
                step(workerBId, "SEQUENTIAL", "output contains APPROVED", 0, 60)));
        given(agentService.runWorker(eq(orgId), eq(workerA), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "lead APPROVED by scoring", 3));
        given(agentService.runWorker(eq(orgId), eq(workerB), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "drafted outreach", 4));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalTokens()).isEqualTo(7);
        verify(agentService, times(1)).runWorker(eq(orgId), eq(workerB), anyString(), any());
    }

    @Test
    void conditionMatcherIsCaseInsensitiveAndFailsClosedOnUnknownSyntax() {
        assertThat(service.conditionMatches("output contains approved", "Lead APPROVED")).isTrue();
        assertThat(service.conditionMatches("output contains approved", "lead rejected")).isFalse();
        assertThat(service.conditionMatches(null, "anything")).isTrue();
        assertThat(service.conditionMatches("", "anything")).isTrue();
        // unknown condition syntax does not silently block the workflow
        assertThat(service.conditionMatches("magic word xyz", "anything")).isTrue();
    }

    // ---------- §8 failure + bounded retry ----------

    @Test
    void workerRetrySucceedsOnSecondAttempt() {
        orch.setMaxRetriesPerStep(2);
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 2, 60)));
        given(agentService.runWorker(eq(orgId), any(), anyString(), any()))
                .willReturn(workerRun("FAILED", "transient provider error", 2),
                        workerRun("COMPLETED", "recovered", 3));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getOutput()).isEqualTo("recovered");
        assertThat(result.getStepsJson()).contains("\"attempts\":1");
        verify(agentService, times(2)).runWorker(eq(orgId), any(), anyString(), any());
    }

    @Test
    void permanentlyFailingWorkerIsBoundedAndFailsTheRun() {
        orch.setMaxRetriesPerStep(2);
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 2, 60)));
        given(agentService.runWorker(eq(orgId), any(), anyString(), any()))
                .willReturn(workerRun("FAILED", "provider down", 1));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getError()).isEqualTo("provider down");
        assertThat(result.getFinishedAt()).isNotNull();
        // 1 initial attempt + 2 bounded retries, never more
        verify(agentService, times(3)).runWorker(eq(orgId), any(), anyString(), any());
        // the second step is never reached
        verify(agentService, never()).runWorker(eq(orgId), eq(workerB), anyString(), any());
    }

    @Test
    void retryCountIsHardCappedEvenWhenConfiguredHigher() {
        orch.setMaxRetriesPerStep(99);
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 99, 60)));
        given(agentService.runWorker(eq(orgId), any(), anyString(), any()))
                .willReturn(workerRun("FAILED", "always failing", 1));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        // cap is 3 retries => 4 attempts total, so no runaway loop is possible
        verify(agentService, times(4)).runWorker(eq(orgId), any(), anyString(), any());
    }

    @Test
    void workerTimeoutIsReportedAsAFailedStep() {
        orch.setMaxRetriesPerStep(0);
        // the orchestrator floors worker timeouts at 10s, so we must exceed that
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 0, 10)));
        given(agentService.runWorker(eq(orgId), any(), anyString(), any()))
                .willAnswer(inv -> {
                    Thread.sleep(11_000L);
                    return workerRun("COMPLETED", "too late", 1);
                });

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getError()).contains("timed out");
    }

    @Test
    void orchestrationDoesNotExecuteWorkerWhenGuardrailRejectsInvocation() {
        // §6: each worker invocation re-verifies tenant/user/agent independently
        willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Agent is disabled"))
                .given(guardrails).checkInvocation(eq(orgId), any(Agent.class), nullable(String.class));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getError()).contains("Agent is disabled");
        // authorization failures are terminal - no retry storm
        verify(agentService, never()).runWorker(eq(orgId), any(), anyString(), any());
    }

    @Test
    void usageLimitExceededStopsTheOrchestrationWithoutRetrying() {
        willThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Tenant AI usage limit exceeded"))
                .given(guardrails).checkInvocation(eq(orgId), any(Agent.class), nullable(String.class));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getError()).contains("usage limit exceeded");
        verify(agentService, never()).runWorker(eq(orgId), any(), anyString(), any());
    }

    // ---------- §6 guardrail is re-checked for EVERY worker invocation ----------

    @Test
    void everyWorkerInvocationReVerifiesTenantAndAgentIndependently() {
        // "Supervisor is trusted, therefore worker is trusted" is NOT assumed:
        // each worker gets its own checkInvocation() with the same tenant.
        given(agentService.runWorker(eq(orgId), any(), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "ok", 1));

        run();

        ArgumentCaptor<Agent> agents = ArgumentCaptor.forClass(Agent.class);
        verify(guardrails, times(2)).checkInvocation(eq(orgId), agents.capture(), nullable(String.class));
        assertThat(agents.getAllValues()).containsExactly(workerA, workerB);
    }

    @Test
    void secondWorkerIsBlockedWhenItsOwnGuardrailCheckFails() {
        given(agentService.runWorker(eq(orgId), eq(workerA), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "lead-42 qualified", 4));
        willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks permission for agent"))
                .given(guardrails).checkInvocation(eq(orgId), eq(workerB), nullable(String.class));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getError()).contains("lacks permission");
        verify(agentService, never()).runWorker(eq(orgId), eq(workerB), anyString(), any());
    }

    // ---------- §11 tenant isolation ----------

    @Test
    void orchestrationCannotBeRunFromAnotherTenant() {
        given(orchestrationRepository.findByOrganizationIdAndId(eq(otherOrgId), any(UUID.class)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.run(otherOrgId, UUID.randomUUID(), "steal data"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Orchestration not found");
        verify(agentService, never()).runWorker(any(), any(), anyString(), any());
    }

    // ---------- §3 supervisor/worker config cannot escalate ----------

    @Test
    void stepCannotReferenceAnAgentTheTenantDoesNotOwn() {
        AgentOrchestration foreign = new AgentOrchestration(orgId, "Escalation");
        foreign.setMaxSteps(5);
        foreign.setStepsJson(stepsJson(step(UUID.randomUUID(), "SEQUENTIAL", null, 0, 60)));
        given(agentService.get(eq(orgId), any(UUID.class)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        assertThatThrownBy(() -> service.create(orgId, foreign))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Agent not found");
    }

    @Test
    void supervisorCannotAlsoBeAWorkerToPreventRecursiveFanOut() {
        Agent supervisor = new Agent(orgId, "Supervisor", null, "coordinate", null);
        ReflectionTestUtils.setField(supervisor, "id", supervisorId);
        given(agentService.get(eq(orgId), eq(supervisorId))).willReturn(supervisor);

        AgentOrchestration recursive = new AgentOrchestration(orgId, "Recursive");
        recursive.setMaxSteps(5);
        recursive.setSupervisorAgentId(supervisorId);
        recursive.setStepsJson(stepsJson(step(supervisorId, "SEQUENTIAL", null, 0, 60)));

        assertThatThrownBy(() -> service.create(orgId, recursive))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Supervisor cannot also be a worker");
    }

    @Test
    void stepCountCannotExceedTheConfiguredExecutionLimit() {
        AgentOrchestration tooWide = new AgentOrchestration(orgId, "Too wide");
        tooWide.setMaxSteps(1);
        tooWide.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 0, 60),
                step(workerBId, "SEQUENTIAL", null, 0, 60)));

        assertThatThrownBy(() -> service.create(orgId, tooWide))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many steps");
    }

    @Test
    void workflowWithoutStepsIsRejected() {
        AgentOrchestration empty = new AgentOrchestration(orgId, "Empty");
        empty.setStepsJson("[]");

        assertThatThrownBy(() -> service.create(orgId, empty))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Need at least one step");
    }

    // ---------- §7 human-in-the-loop pause + safe resume ----------

    @Test
    void highImpactWorkerStepPausesTheRunAndSkipsRemainingWorkers() {
        UUID approvalId = UUID.randomUUID();
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 2, 60),
                step(workerBId, "SEQUENTIAL", null, 2, 60)));
        given(agentService.runWorker(eq(orgId), eq(workerA), anyString(), any()))
                .willReturn(workerRun("WAITING_APPROVAL", "Approval required: " + approvalId, 7));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("WAITING_APPROVAL");
        assertThat(result.getError()).contains(approvalId.toString());
        // paused, not finished
        assertThat(result.getFinishedAt()).isNull();
        assertThat(result.getStepsJson()).contains("WAITING_APPROVAL");
        // tokens burned before the pause are still recorded for usage/cost
        assertThat(result.getTotalTokens()).isEqualTo(7);
        // no downstream worker may run while the approval is pending
        verify(agentService, never()).runWorker(eq(orgId), eq(workerB), anyString(), any());
    }

    @Test
    void approvingThePauseResumesOnlyTheRemainingWorkers() {
        UUID runId = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();

        AgentApproval approved = new AgentApproval(orgId, null, runId,
                workerAId, "Qualifier", "send_email", "{}", "tester");
        ReflectionTestUtils.setField(approved, "id", approvalId);
        approved.setStatus("APPROVED");
        approved.setDecidedBy("manager");
        given(approvals.list(orgId, "APPROVED")).willReturn(List.of(approved));

        OrchestrationRun paused = new OrchestrationRun(orgId, UUID.randomUUID(), "Lead flow",
                "tester", "find new leads");
        ReflectionTestUtils.setField(paused, "id", runId);
        paused.setStatus("WAITING_APPROVAL");
        paused.setStepsJson("""
                [{"index":0,"status":"COMPLETED","output":"lead-42 qualified","tokens":4},
                 {"index":1,"status":"WAITING_APPROVAL","output":"Approval required: %s","tokens":0}]
                """.formatted(approvalId));
        given(runRepository.findById(runId)).willReturn(Optional.of(paused));
        given(agentService.runWorker(eq(orgId), eq(workerB), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "draft sent", 6));

        OrchestrationRun result = service.resumeAfterApproval(orgId, approvalId);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getOutput()).isEqualTo("draft sent");
        assertThat(result.getTotalTokens()).isEqualTo(6);
        // the already-completed step is never replayed
        verify(agentService, never()).runWorker(eq(orgId), eq(workerA), anyString(), any());
        verify(agentService, times(1)).runWorker(eq(orgId), eq(workerB), anyString(), any());
    }

    @Test
    void rejectedApprovalDoesNotResumeTheWorkflow() {
        given(approvals.list(orgId, "APPROVED")).willReturn(List.of());

        assertThatThrownBy(() -> service.resumeAfterApproval(orgId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not in APPROVED state");
        verify(agentService, never()).runWorker(any(), any(), anyString(), any());
    }

    // ---------- §4 bounded parallel execution (PARALLEL_SAFE) ----------

    @Test
    void parallelSafeStepsRunIndependentlyAndAggregateTheirUsage() {
        orch.setExecutionMode("PARALLEL_SAFE");
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 0, 60),
                step(workerBId, "SEQUENTIAL", null, 0, 60)));
        given(agentService.runWorker(eq(orgId), eq(workerA), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "branch-a", 3));
        given(agentService.runWorker(eq(orgId), eq(workerB), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "branch-b", 7));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalTokens()).isEqualTo(10);
        assertThat(result.getEstimatedCostMicros()).isEqualTo(20L);
        assertThat(result.getFinishedAt()).isNotNull();

        ArgumentCaptor<String> ctx = ArgumentCaptor.forClass(String.class);
        verify(agentService, times(2)).runWorker(eq(orgId), any(), ctx.capture(), any());
        // §5: no branch ever receives a sibling branch's output
        assertThat(ctx.getAllValues()).containsOnly("find new leads");
    }

    @Test
    void parallelSafeModeReportsFailureWhenAnyWorkerFails() {
        orch.setExecutionMode("PARALLEL_SAFE");
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 0, 60),
                step(workerBId, "SEQUENTIAL", null, 0, 60)));
        given(agentService.runWorker(eq(orgId), eq(workerA), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "ok", 1));
        given(agentService.runWorker(eq(orgId), eq(workerB), anyString(), any()))
                .willReturn(workerRun("FAILED", "worker exploded", 1));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getError()).contains("worker exploded");
    }

    @Test
    void parallelSafeExecutionStaysBoundedButProcessesEveryStep() {
        orch.setExecutionMode("PARALLEL_SAFE");
        orch.setMaxSteps(6);
        String[] steps = new String[6];
        for (int i = 0; i < steps.length; i++) {
            steps[i] = step(workerAId, "SEQUENTIAL", null, 0, 60);
        }
        orch.setStepsJson(stepsJson(steps));
        given(agentService.runWorker(eq(orgId), eq(workerA), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "ok", 1));

        OrchestrationRun result = run();

        // the pool is capped (MAX_PARALLEL_STEPS) yet every step still completes
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalTokens()).isEqualTo(6);
        verify(agentService, times(6)).runWorker(eq(orgId), eq(workerA), anyString(), any());
    }

    @Test
    void parallelSafeModePausesWhenAWorkerRequiresApproval() {
        orch.setExecutionMode("PARALLEL_SAFE");
        orch.setStepsJson(stepsJson(step(workerAId, "SEQUENTIAL", null, 0, 60),
                step(workerBId, "SEQUENTIAL", null, 0, 60)));
        given(agentService.runWorker(eq(orgId), eq(workerA), anyString(), any()))
                .willReturn(workerRun("COMPLETED", "ok", 1));
        given(agentService.runWorker(eq(orgId), eq(workerB), anyString(), any()))
                .willReturn(workerRun("WAITING_APPROVAL", "Approval required: abc", 2));

        OrchestrationRun result = run();

        assertThat(result.getStatus()).isEqualTo("WAITING_APPROVAL");
        // a paused workflow keeps no FinishedAt so it can be resumed safely
        assertThat(result.getFinishedAt()).isNull();
    }
}

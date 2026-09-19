# Phase 13 Verification Report (AI agents + guardrail foundation)

Date: 2026-09-19. Verified via automated test suite (`mvn -o clean test` — **42 tests, 0 failures, BUILD SUCCESS**).

## 1. Delivered

**Agent core (per `phase13-plan.md`):**
- `Agent` (table `ai_agents`) + `AgentRun` (table `ai_agent_runs`: output, stepsJson audit, username, model, tokensUsed, estimatedCostMicros), repositories.
- `AgentTool` interface + `AgentToolRegistry` (whitelist resolver); tools now declare `requiredPermission()` and `requiresApproval()`.
- `AgentService`: CRUD + guardrailed bounded run loop (MAX_ITERATIONS=6), `run()` with WAITING_APPROVAL pause, `runWorker(orgId, agent, input, orchRunId)`, `continueAfterApproval`, `resumeWorkerAfterApproval`, `resumeLoop`, `getRun`.
- `AgentController` `/api/ai/agents`: CRUD, `/{id}/run`, `/runs?agentId`, `/tools`.

**Guardrail package `agent/guardrail/`:**
- `AgentGuardrailService` — prompt-injection pattern detection on input and output; per-invocation re-verification (tenant org, agent ownership/enabled, per-agent + tenant token quota `TENANT_TOKEN_QUOTA = 1_000_000`); handoff-context sanitization; usage recording with cost estimate (2 micros/token).
- `AgentApproval` + repository, `AgentApprovalService`, `AgentApprovalController` — `/api/ai/approvals`: list by status, `/{id}/approve`, `/{id}/reject`, `/usage` (tenant token usage).
- `AgentResumeService` — resumes paused single-agent runs after an approval decision.
- `OrchestrationResumeBridge` — forwards approval decisions into waiting orchestration runs.

**Tools added (all org-scoped, permission-gated):** `get_lead`, `update_lead`, `get_client`, `update_task`, `send_email`, `send_notification` (joining `create_lead`, `create_task`, `search_knowledge_base`, `get_services`).

**Frontend:** `ai.tsx` Approvals tab (`ApprovalQueue`) — pending approvals with args preview, approve/reject, tenant token usage display.

## 2. DoD re-verification

| # | Item | Evidence | Result |
|---|------|----------|--------|
| 1 | Injection guardrails (input + output patterns) | `AgentGuardrailService` checks; covered by orchestration test suite | ✅ |
| 2 | "Never trust the caller": per-invocation tenant/agent/quota re-check | `guardrails.checkInvocation(orgId, agent, username)` re-run per worker (verified in `OrchestrationService.runStepWithRetry`) | ✅ |
| 3 | High-impact tools require approval | `requiresApproval()` tools create PENDING `AgentApproval`; run pauses `WAITING_APPROVAL` | ✅ |
| 4 | Approve/reject resumes or safely terminates | `AgentResumeService` (single-agent), `OrchestrationResumeBridge` → `resumeAfterApproval` / `markRejectedAfterDecision` | ✅ |
| 5 | Usage recorded per run with linkage | `AgentUsage` rows per invocation, `orchestrationRunId` linkage, cost 2 micros/token | ✅ |
| 6 | Tenant isolation of agents, runs, approvals, usage | All lookups org-scoped via `UserContext.getOrganizationId()`; cross-tenant tests green | ✅ |

## 3. Status

**Phase 13: DONE.** Backend builds and full test suite green (`mvn -o clean test`: 42 tests, 0 failures). Frontend `tsc -b && vite build` clean.

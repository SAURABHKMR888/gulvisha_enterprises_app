# PHASE 13 — AI AGENTS: PLAN (read this instead of re-scanning)

Date: 2026-09-10. Phase 12 COMPLETE (tenant-onboard-via-UI test passed).

## STATUS (2026-09-11): IMPLEMENTED — backend + seed + frontend. Verified `mvn compile` exit 0 and `tsc --noEmit` clean. Pending: manual runtime tests below.

## What was built (actual file layout)
- `backend/.../agent/`: `Agent` (table ai_agents), `AgentRun` (ai_agent_runs; output + stepsJson audit), `AgentRepository`, `AgentRunRepository`, `AgentTool` interface (execute(orgId, args) -> AgentToolResult), `AgentToolRegistry` (whitelist resolver), `AgentService` (CRUD + bounded run loop, MAX_ITERATIONS=6, per-run audit), `AgentController` (/api/ai/agents: CRUD, /{id}/run, /runs?agentId, /tools).
- Tools in `agent/tool/`: `SearchKnowledgeTool` (RAG, org-scoped), `CreateTaskTool`, `CreateLeadTool` (default pipeline stage, source=agent), `GetServicesTool`.
- `DataInitializer`: seeds one demo agent per tenant — "Gulvisha Sales Assistant" and "ABC Client Intake Assistant" — same engine, tenant-specific prompts, tools = search_knowledge_base,create_lead,create_task,get_services.
- `frontend/src/ai.tsx`: new "Agents" tab (`AgentManager`): list/create/edit agents (system prompt + tool checkboxes + enable/disable), Run panel with status/output/tool steps, recent run history.

## How to test (manual)
1. Restart backend (seeds run). As `gulvisha-admin` -> AI -> Agents: "Gulvisha Sales Assistant" present.
2. Run with e.g. "Jane Doe jane@corp.com wants bulk data entry pricing" -> expect search_knowledge_base + create_lead steps; lead appears in Gulvisha CRM (source=agent, stage NEW).
3. As `abc-admin`: only "ABC Client Intake Assistant" visible; its run lands leads in ABC's CRM/pipeline — never Gulvisha's.
4. Isolation: GET /api/ai/agents/{gulvisha-agent-id} as abc-admin -> 404. RAG regression: chat knowledge search still tenant-isolated (Phase 11).
5. Run history persists across reloads.

## Remaining (future phases)
- Multi-agent orchestration (P14), MCP tools (P15), richer agent tools e.g. email/WhatsApp (P16).

## Audit — what exists (verified)
- AI module: `ai/entity/AiConfiguration` (1:1 per org: provider/model/apiKey/baseUrl/enabled/temperature/maxTokens), AiPrompt, AiConversation, AiMessage; `AiProviderFactory` + providers; `RagService` (org-isolated RAG, Phase 11).
- `AiController` (/api/ai/**): config, prompts, chat, conversations, knowledge CRUD + search. All resolve org via `UserContext.getOrganizationId()`.
- RBAC: `Permission.AI_AGENT_EXECUTE ("ai:agent:execute")` EXISTS (PLATFORM_ADMIN, ORGANIZATION_ADMIN, AGENT roles) but NOTHING implements it — no Agent entity/service/endpoint.
- Workflow engine pattern to mirror: `workflow/WorkflowAction` interface (key() + execute(orgId, params)) + registry + org-ownership checks.
- NO agent code exists: no AiAgent entity, no tool registry, no agent runtime.

## Design (single source of truth, tenant-aware)
- `AiAgent` entity (table ai_agents): id, organizationId, name, description, systemPrompt (instructions), tone, enabled, allowedTools (jsonb string[]), createdAt/updatedAt. Unique (organizationId, name).
- `AiAgentRun` entity (table ai_agent_runs): audit of every execution: agentId, organizationId, username, input, output, toolCalls (jsonb), tokensUsed, status, createdAt.
- `AgentTool` interface (mirror WorkflowAction): key(), description(), permission(), execute(orgId, username, params) -> result. Registry = Spring-injected List<AgentTool>.
- Tools (Phase 13 set, permission-controlled, org-scoped): CREATE_TASK, CREATE_LEAD, SEARCH_KNOWLEDGE, GET_SERVICES, ADD_NOTE. No arbitrary DB access.
- Runtime `AgentService.run(agentId, input)`: load agent -> org ownership + enabled + AI config enabled -> system prompt (agent.systemPrompt + org context) -> LLM tool-calling loop (bounded, max 5 tool rounds) -> execute tools gated by agent.allowedTools AND user permission -> persist AiAgentRun.

## API (all under /api/ai/agents, gated ai:agent:execute / ai:use)
- GET  /api/ai/agents            list (org)
- POST /api/ai/agents            create
- PUT  /api/ai/agents/{id}       update
- DELETE /api/ai/agents/{id}
- GET  /api/ai/agents/tools      registry (key/description/permission)
- POST /api/ai/agents/{id}/run   execute with input (audited)
- GET  /api/ai/agents/{id}/runs  run history

## Files
NEW: ai/entity/AiAgent.java, AiAgentRun.java; ai/repository/AiAgentRepository.java, AiAgentRunRepository.java; ai/dto/AiAgentDto.java, AiAgentRunDto.java, AiAgentRunRequest.java; ai/agent/AgentTool.java, AgentToolRegistry.java, tools/CreateTaskTool.java, CreateLeadTool.java, SearchKnowledgeTool.java, GetServicesTool.java, AddNoteTool.java; ai/service/AgentService.java; ai/controller/AgentController.java.
MODIFY: DataInitializer (seed 1 demo agent per tenant), frontend ai.tsx (Agents tab), docs summary.
NO CHANGE: providers, RagService, chat, workflow engine, security model (permissions already exist).

## Order
1. Entities + repos + DTOs  2. AgentTool + registry + 5 tools  3. AgentService (LLM + bounded tool loop)  4. AgentController  5. Seed demo agents  6. Frontend Agents tab  7. Tests: run agent per tenant, cross-tenant isolation (abc agent cannot touch gulvisha data), RAG regression.

## DoD
- Tenant admin can create/edit agent + pick tools via UI, no code change.
- Agent run executes tools only in allowedTools AND user permission; all runs audited; org isolation everywhere.
- RAG still isolated (Phase 11 regression pass).

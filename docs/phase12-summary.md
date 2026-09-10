# Gulvisha Platform — Continuation Summary (keep updated; read this INSTEAD of re-scanning the codebase)

Last updated: 2026-09-09 · Phase 12 architecture VERIFIED with two tenants (see docs/phase12-verification.md). UI-config gaps remain (see below).

## What this project is
Spring Boot 3.5 (Java 21, package `com.gulvisha.backend`) + React/Vite TS frontend, PostgreSQL (`gulvisha` DB). Becoming a generic multi-tenant SaaS: one codebase, tenant data/config drives everything. Gulvisha = first tenant (seed data only).

## Phase history
- Phases 1-10: auth/JWT/RBAC, enquiries, admin dashboard, org settings, services, pipeline stages, custom fields, users, client portal, workflows, AI module (multi-provider chat, config, API keys, mode toggle).
- Phase 11 (DONE): RAG — `ai_knowledge_documents` + `ai_knowledge_chunks` (embedding = jsonb float[]), RagService (chunk ~800c/overlap 120, cosine in Java, MIN_SIMILARITY=0.45), provider `embed()` (gemini-embedding-001 / nomic-embed-text / text-embedding-3-small), chat KB modes: auto/strict/general (`knowledgeBaseMode` in AiChatRequestDto), Knowledge tab in ai.tsx, sources in replies. Verified end-to-end.
- Phase 12 (IN PROGRESS): Step 1 audit → `docs/phase12-audit.md`. Steps 2–4 COMPLETE: Organization extended (slug, displayName, logoUrl, faviconUrl, primaryColor, accentColor), public `GET /api/public/site`, SecurityConfig permits it, OrganizationController PUT persists new fields + removed first-org fallback, DataInitializer seeds branding + siteContent JSONB (hero/about/industries/process/quote). Frontend: `siteConfig.ts` (fetch + apply CSS vars/title/favicon), `--brand-primary`/`--brand-accent`, admin-layout dynamic brand, App.tsx public site uses config + fetches services. Steps 5–6 DONE (services data-driven; Service/Pipeline/QuoteRequest/Organization controllers use UserContext + slug + ownership checks). Step 16 DONE: ABC Consulting seeded (slug=abc, abc-admin/abc123, audit/tax services, custom pipeline, navy/gold, own siteContent). **TWO-TENANT ARCHITECTURE VERIFIED LIVE (docs/phase12-verification.md).**
- Phase 12 UI gaps (product completeness): (1) settings.tsx Org tab must render displayName/slug/logoUrl/faviconUrl/primaryColor/accentColor; (2) add Website tab to edit siteContent; (3) workflows.tsx hardcoded trigger/action arrays → registry-driven; (4) AiConfiguration persona/tone fields (optional); (5) re-test tenant-C via UI only = success criterion.

## Architecture facts (verified)
- Tenant mechanism: JWT claims → `security/UserContext` (orgId, clientId for CLIENT role, role). `config/TenantContext.java` is DEAD CODE.
- All entities org-scoped. `PortalService` enforces org+clientId (portal isolation ✓). Workflow engine (`workflow/WorkflowEngine`) is generic: triggers + registered WorkflowAction beans + execution history; only 2 actions exist (CREATE_LEAD, ADD_NOTE).
- Organization entity has: name, displayName, slug, description, industry, email, phone, website, address, timezone, currency, language, logoUrl, faviconUrl, primaryColor, accentColor, status.
- SecurityConfig: public = /api/auth/**, /api/health, POST /api/enquiries + /api/quote-requests, GET /api/services. Everything else permission-gated (PERMISSION_*).
- DataInitializer seeds Gulvisha tenant (org, admin/change-me, 7 services, NEW→LOST pipeline, custom fields, leads, ACME client, project/tasks, portal user client/client123, "Enquiry follow-up" workflow).

## Phase 12 audit — key findings (full detail in docs/phase12-audit.md)
- 🔴 CRITICAL A1-A4: public/admin endpoints resolve tenant as "first org in DB": QuoteRequestService.getDefaultOrganizationId(), ServiceController, PipelineController (update/delete also lack ownership checks!), OrganizationController.resolveOrganizationId() fallback. Fix = org `slug` + TenantResolver + UserContext everywhere.
- 🟠 HIGH: public website 100% hardcoded Gulvisha in App.tsx/sitePages.tsx/home.tsx (hero, 3 services, industries, process, capabilities, hello@gulvisha.com, footer, lead-source options); index.html title/meta; admin-layout.tsx sidebar "Gulvisha"/"G"; styles.css has no CSS variables; enquiry statuses hardcoded in admin.tsx + QuoteRequestService dashboard + styles.css classes; EnquiryNotificationService hardcodes "New Gulvisha enquiry" + no-reply@gulvisha.local + global MAIL_* env.
- 🟡 MEDIUM: workflows.tsx hardcodes trigger/action option arrays; AiConfiguration lacks persona/instructions fields; HealthController payload says "Gulvisha Enterprises".
- ⚪ LOW: TenantContext dead code; dev defaults in application.yml (DB pwd "postgress" typo, JWT default); keep com.gulvisha.* package names for MVP.
- ✅ Already tenant-ready: all entities org-scoped; RAG/AI strict isolation; workflow engine generic; settings.tsx CRUD (Services/Pipeline/Custom fields); LoginPage/portal.tsx generic; JWT permission matchers.

## Agreed plan (order)
1. ✅ Steps 2-3: slug+branding on Organization → public GET /api/public/site → CSS vars + dynamic title/logo
2. Step 4: public website content (homepage hero, about, industries, process, FAQ) — tenant-configurable
3. Step 5: service displayOrder/visibility + TenantResolver fixes A1-A4
4. Step 6: enquiry status + lead-source config
5. Steps 7-8: portal field audit
6. Step 9: workflow registry endpoint + generic actions (SEND_EMAIL, CREATE_TASK, AI_ACTION, WEBHOOK, conditions)
7. Steps 10-11: AI persona fields + cross-tenant RAG test
8. Step 14: settings tabs · 9. Step 15: hardcode sweep · 10. Step 16: ABC Consulting demo tenant (mandatory proof)

## Run / verify commands
- Backend: `java -jar backend\gulvisha-backend\target\gulvisha-backend-0.0.1-SNAPSHOT.jar` (build: `E:\maven\apache-maven-3.9.6\bin\mvn.cmd -q -DskipTests package`). Frontend: `npm run dev` in frontend\gulvisha-frontend (proxy /api → :8080).
- Login: admin / change-me. Health: GET :8080/api/health. psql: `C:\Program Files\PostgreSQL\18\bin\psql.exe -U postgres -d gulvisha` (password NOT "postgres" — check user's env).
- NOTE: user may run backend via `mvn spring-boot:run` themselves — always check `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` for duplicate instances on 8080 before testing (stale-JVM confusion happened twice).
- Editor tool fails on CRLF files → use PowerShell `[IO.File]::ReadAllText/WriteAllText` with UTF8Encoding($false) for edits to pre-existing Java/TSX files.
- Terminal shell integration is flaky ("command may still be running" noise) — outputs are still visible in terminal content; prefer Start-Process -Wait + log files for builds.

## Docs
- `docs/phase12-audit.md` — full Step-1 audit report (deliverable).
- `docs/architecture.md`, `README.md` — updated through Phase 11.

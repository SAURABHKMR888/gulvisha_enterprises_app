# PHASE 12 — STEP 1: CODEBASE AUDIT (Generic Multi-Tenant Readiness)

Date: 2026-09-08 · Scope: full backend + frontend · **No code modified in this step.**
Severity: 🔴 CRITICAL = blocks multi-tenancy · 🟠 HIGH = wrong data/behaviour per tenant · 🟡 MEDIUM = tenant content/branding · ⚪ LOW = cosmetic/naming

## Verdict
The platform is architecturally CLOSE to multi-tenant: every business entity carries `organizationId`, RAG/AI/workflows/portal are org-scoped, and Settings already manages Services/Pipeline/Custom Fields as data. The main gaps are (1) **public/unauthenticated endpoints resolve tenant as "first org in DB"**, (2) **the whole public website is hardcoded Gulvisha content in React**, (3) **branding fields don't exist on Organization**, (4) **enquiry statuses are hardcoded platform-wide**, and (5) **email notifications hardcode Gulvisha + global env config**.

## A. Tenant resolution — 🔴 CRITICAL (multi-tenant blockers)
| # | File | Hardcoded behaviour | Why tenant-specific | Recommended model | Refactor? |
|---|------|--------------------|---------------------|-------------------|-----------|
| A1 | `service/QuoteRequestService.java` (L37-42, L54, L68) | `getDefaultOrganizationId()` = **first org in DB**; used by public `POST /api/quote-requests` + `POST /api/enquiries` | Every public enquiry from every future tenant lands in org #1 | Add `slug` (unique) to Organization; public endpoints accept tenant context (subdomain/slug/site key) via `TenantResolver` | Yes |
| A2 | `controller/ServiceController.java` (L28-32 + create/update) | Public `GET /api/services` + admin CRUD resolve **first org** | Tenant B sees tenant A's services; CRUD writes to org #1 | Authenticated → `UserContext`; public GET → tenant resolver | Yes |
| A3 | `controller/PipelineController.java` (L24-29 + update/delete) | Same first-org pattern; **update/delete don't verify org ownership** (cross-tenant write risk) | Pipeline stages are per-tenant config | Use `UserContext` + ownership check on every mutation | Yes |
| A4 | `controller/OrganizationController.java` (L53-62) | `resolveOrganizationId()` falls back to first org when context missing | Admin of any tenant could read/update org #1 in a multi-org DB | Require `UserContext.getOrganizationId()`; delete fallback | Yes |

## B. Branding / identity — 🟠 HIGH (data model gaps)
| # | File | Finding | Recommended model | Refactor? |
|---|------|---------|-------------------|-----------|
| B1 | `organization/Organization.java` | Has: name, description, industry, email, phone, website, address, timezone, currency, language, status. **Missing: logo, favicon, displayName, primaryColor, accentColor, slug** | Extend Organization + new **public** `GET /api/public/site/{slug}` | Yes — Steps 2-3 |
| B2 | frontend `index.html` | `<title>Gulvisha Enterprises \| Business Solutions</title>` + meta description | Set at runtime from tenant config | Yes |
| B3 | `admin-layout.tsx` (L24-27) | Sidebar brand "Gulvisha" + logo letter "G" | From org config | Yes |
| B4 | `styles.css` | Palette as literal hex values (no CSS variables) | CSS custom properties seeded from tenant theme | Yes |

## C. Public website content — 🟠 HIGH (all hardcoded in React)
| # | File | Hardcoded content | Refactor? |
|---|------|-------------------|-----------|
| C1 | `App.tsx` (L32-108, L183-185, L304, L308) | Hero "Technology, outsourcing and AI solutions…", "Gulvisha Enterprises helps teams…", 3 services, `serviceOptionsByCategory`, industries, processSteps, capabilities, footer, error "email hello@gulvisha.com" | Yes — Steps 4-5 |
| C2 | `sitePages.tsx` (L7-33, L86, L99) | Services/process pages, "ABOUT GULVISHA", contact `hello@gulvisha.com` | Yes |
| C3 | `home.tsx` (L26-34) | Hero copy, "GULVISHA / 2026" | Yes |
| C4 | `App.tsx` (L301) | Lead-source options (Website/LinkedIn/Upwork/…) hardcoded | Lead sources → tenant config (Step 6) |

## D. CRM / statuses — 🟠 HIGH
| # | File | Finding | Recommended model | Refactor? |
|---|------|---------|-------------------|-----------|
| D1 | `frontend/admin.tsx` (L77, L274-287) + `styles.css` (L165-208) | Enquiry statuses NEW/QUALIFIED/IN_PROGRESS/CLOSED/REJECTED/ARCHIVED hardcoded in metrics/filters/CSS classes | Statuses as tenant config (PipelineStage-like), CSS class mapping derived from config | Yes — Step 6 |
| D2 | `service/QuoteRequestService.java` (L162-170) | Dashboard counts hardcoded to same statuses | Count from configured statuses | Yes |
| D3 | `crm/Lead.java` (L33-39) | `service`/`source`/`status` free strings, default "NEW" | Acceptable short-term; optionally FK later | Later |
| D4 | `DataInitializer.java` (L97-105) | Pipeline NEW→LOST seeded | ✅ OK as Gulvisha seed; ABC gets different seed (Step 16) | No |

## E. Notifications / email — 🟠 HIGH
| # | File | Finding | Recommended model | Refactor? |
|---|------|---------|-------------------|-----------|
| E1 | `service/EnquiryNotificationService.java` (L26, L42-48) | Subject "**New Gulvisha enquiry**", from `no-reply@gulvisha.local`, recipient = global env var, static body | Per-tenant notification settings (from/to/subject template), org email as default recipient; template table later | Yes — Steps 2/6 |

## F. Workflow — engine generic ✓, surface limited (🟡 MEDIUM)
| # | File | Finding | Recommendation |
|---|------|---------|----------------|
| F1 | `workflow/WorkflowEngine.java` | ✅ Generic: org-scoped triggers, registered action handlers, execution history, failure-safe. **Do NOT rewrite.** | Extend only |
| F2 | `workflow/AddNoteAction.java`, `CreateLeadAction.java` | Only 2 registered actions | Add generic actions: SEND_EMAIL, CREATE_TASK, UPDATE_STATUS, AI_ACTION, WEBHOOK + condition step |
| F3 | `frontend/workflows.tsx` (L42-43) | `workflowTriggers=['ENQUIRY_CREATED','MANUAL']`, `stepActions=['CREATE_LEAD','ADD_NOTE']` hardcoded | Serve registry from backend `GET /api/workflows/registry` |

## G. AI / RAG — ✅ tenant-isolated by design (🟡 enhancements)
| # | File | Finding |
|---|------|---------|
| G1 | `ai/*` | Config, prompts, conversations, knowledge docs/chunks all keyed by `organizationId`; retrieval org-scoped ✓; chat preambles generic (no Gulvisha text) ✓ |
| G2 | `ai/entity/AiConfiguration.java` | Missing tenant AI **persona/instructions/tone** fields → add (Step 10) |
| G3 | RAG isolation | Explicit cross-tenant retrieval test required (Step 11) |

## H. Platform hygiene — ⚪ LOW
| # | File | Finding | Action |
|---|------|---------|--------|
| H1 | `config/TenantContext.java` | **Dead code** (zero usages) — `UserContext` is the real mechanism | Delete or adopt; don't duplicate |
| H2 | `controller/HealthController.java` | Health payload hardcodes "Gulvisha Enterprises" | Make neutral/configurable |
| H3 | `application.yml` | Dev defaults: DB password `postgress` (typo), default JWT secret | Document/override in prod (Phase 17) |
| H4 | Package names `com.gulvisha.*`, artifact `gulvisha-backend`, repo paths | Platform naming, NOT tenant logic | **Keep** for MVP — renaming is churn with no tenant value |
| H5 | `DataInitializer.java` (L58-234) | Seeds: org, admin/client users, 7 services, pipeline, custom fields, leads, ACME client, project/tasks, BPO resources, workflow | ✅ Acceptable as Gulvisha seed; add ABC tenant seed (Step 16) |
| H6 | `UserRepository.findByUsername(...)` | Usernames appear globally unique → same username can't exist in two tenants | Verify AuthService; decide globally-unique (OK for MVP) vs per-org |
| H7 | `backend/src/test/*` | Tests reference "AI & Automation" etc. | ✅ Test fixtures — acceptable |

## Already tenant-ready (verified, no changes needed)
- All business entities carry `organizationId`; repositories expose org-scoped finders
- `PortalService` enforces org + clientId from JWT (portal isolation on backend) ✓
- `AiService`/`RagService`/knowledge base: strict org scoping ✓ (Phase 11)
- `WorkflowEngine`: org-scoped, config-driven, execution history ✓
- `settings.tsx`: Organization profile + Services/Pipeline/Custom-Fields CRUD already data-driven ✓
- `LoginPage`: generic wording ✓; `portal.tsx`: uses client profile name ✓
- JWT carries orgId + clientId + role; `SecurityConfig` permission matchers per area ✓

## Suggested implementation order (after audit sign-off)
1. **Steps 2-3**: `slug` + branding fields on Organization → public `GET /api/public/site/{slug}` → frontend theme via CSS vars + dynamic title/logo
2. **Steps 4-5**: `site_content` (JSONB sections) + services displayOrder/visibility → refactor App/sitePages/home to fetch; `TenantResolver` fixes A1-A4
3. **Step 6**: enquiry status config + lead sources (admin.tsx + QuoteRequestService refactor)
4. **Steps 7-8**: portal field-level audit (documents/tickets when built)
5. **Step 9**: workflow registry endpoint + generic actions (incremental)
6. **Steps 10-11**: AI persona fields + cross-tenant RAG test
7. **Step 14**: settings UI tabs for new config
8. **Step 15**: hardcoding sweep (only seeds/tests/docs may keep Gulvisha)
9. **Step 16**: ABC Consulting demo tenant + dual-tenant end-to-end verification


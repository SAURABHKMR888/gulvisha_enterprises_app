# Phase 12 Verification Report (two-tenant live test)

Date: 2026-09-09. Verified via backend REST (curl). Backend JAR running on :8080.

## 1. Passed — verified live, both tenants

| # | Check | Gulvisha (admin/change-me) | ABC Consulting (abc-admin/abc123) | Result |
|---|-------|-----------------------------|-----------------------------------|--------|
| 1 | Public site `/api/public/site` | Gulvisha, slug=gulvisha, #315941/#c94f2c, BPO/IT/AI copy | ABC Consulting, slug=abc, #1e3a5f/#d4a843, accounting copy | ✅ fully distinct |
| 2 | Public services `/api/services?slug=` | BPO/IT/AI catalog | Audit/Tax/Advisory catalog | ✅ distinct catalogs |
| 3 | Login | 200 JWT | 200 JWT | ✅ |
| 4 | Pipeline `/api/pipeline-stages` | NEW→…→LOST (org ad687…) | New Lead→…→Declined (org dfb63…) | ✅ distinct + org-scoped |
| 5 | Users `/api/users` | admin + client | abc-admin only | ✅ org-scoped |
| 6 | Cross-tenant direct-ID read | — | ABC GET Gulvisha stage → **403** | ✅ blocked |
| 7 | AI config `/api/ai/config` | gemini, enabled, key | ollama, disabled | ✅ isolated |
| 8 | Knowledge `/api/ai/knowledge` | Return Policy (READY) | empty | ✅ isolated |

**Backend verdict: strictly multi-tenant. No first-org fallbacks remain in Service/Pipeline/QuoteRequest/Organization paths — all use UserContext or slug.**

## 2. Not complete — product/UI gaps vs Phase 12 checklist

Backend already supports these; the **Settings UI does not expose them**, so a tenant admin can't configure them through the UI yet.

| Checklist item | Backend | Settings UI | Gap |
|----------------|---------|-------------|-----|
| Organisation (name, industry, email, phone, website, address, tz, currency, lang) | ✅ PUT /api/organizations/current | ✅ Organization tab | none |
| Branding (displayName, slug, logo, favicon, primary/secondary colour) | ✅ fields + PUT persist | ❌ not rendered in Org tab | **GAP: add fields** |
| Website content editor (hero/about/services copy/industries/process/quote) | ✅ `siteContent` JSONB + PUT | ❌ no editor | **GAP: add Website tab** |
| Business config (services, pipeline, custom fields) | ✅ CRUD | ✅ tabs | none |
| Workflow config UI | ✅ CRUD + engine | ⚠️ workflows.tsx exists, but trigger/action options hardcoded (`ENQUIRY_CREATED`, `MANUAL`, `CREATE_LEAD`, `ADD_NOTE`) | **GAP: registry-driven options** |
| AI instructions / tone / persona | ⚠️ AiPrompt per chat; AiConfiguration lacks persona fields | ✅ config tab (provider/model/key/temp/tokens) | **GAP (audit G2)** |
| Client portal config | portal generic | — | not required for MVP test |

**Most important product gap: ABC Consulting was seeded via DataInitializer (a code change). The Phase 12 success criterion is "configure a new tenant entirely through the UI, no source changes."** Services + pipeline can be added via UI today; branding, colours, and website copy can only be set via API/seed until the UI exposes them.

## 3. Minimal work to close the product gap (frontend-only; backend already ready)

1. `settings.tsx` Organization tab → add displayName, slug, primaryColor, accentColor, logoUrl, faviconUrl inputs.
2. New `Website` tab → field-by-field editor for `siteContent` JSON (with advanced JSON textarea), saved via existing PUT.
3. `workflows.tsx` → replace hardcoded arrays with registry-driven options (lower priority).
4. Re-test: create tenant C via UI alone → branding/colours/site copy change public site + admin chrome with zero code edits.

## 4. Status

- Phase 12 architecture/backend: **DONE + verified**.
- Phase 12 product/UI completeness: **NOT done** until §3 gaps are closed.
- Do not start Phase 13 until the "configure tenant via UI only" test passes.
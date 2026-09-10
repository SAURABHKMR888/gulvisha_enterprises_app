# Architecture

The application begins as a modular monolith:

```text
React + TypeScript (Vite)
          |
          | /api
          v
Spring Boot REST API
          |
          v
Service / repository layers
          |
          v
PostgreSQL
```

## Implemented modules

- Public React landing page and enquiry form
- Spring Boot health, enquiry submission, and admin APIs
- Quote request service, repository, and PostgreSQL entity persistence
- JWT authentication via `/api/auth/login` (Bearer tokens)
- Role-based access control for `/api/admin/**` (ADMIN, MANAGER, VIEWER)
- React admin dashboard with summary metrics, filtering, and enquiry status updates
- Admin-only internal enquiry notes for follow-up context
- Admin search, date filtering, sorting, pagination, CSV export, bulk status updates, and non-destructive archiving
- AI module: configurable providers (Ollama, Gemini, OpenAI), chat with persisted conversations, and system prompts
- RAG knowledge base: per-organization documents are chunked, embedded with the configured provider, stored as JSONB vectors in PostgreSQL, and the most similar chunks (in-memory cosine similarity) are injected into chat as grounding context with source attribution
- Knowledge-base chat modes (per-message toggle in the chat UI): `auto` (inject context when relevant — default), `strict` (answer only from the knowledge base, refuses when nothing relevant is found, without calling the LLM), and `general` (skip the knowledge base entirely)

PostgreSQL settings are externalized through `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`. Admin credentials are externalized through `ADMIN_USERNAME`, `ADMIN_PASSWORD`, or multi-user `ADMIN_USERS` role entries. JWT settings use `JWT_SECRET` and `JWT_EXPIRATION`. Optional mail delivery uses `MAIL_ENABLED` and standard `SPRING_MAIL_*` settings.

Hibernate updates the existing `quote_requests` table with the nullable `internal_notes` column when the backend starts.

Hibernate also creates the `enquiry_audit` table for protected status, notes, and creation history. For production, use managed PostgreSQL, HTTPS, secret environment variables, and a reverse proxy in front of the backend and frontend.

The frontend development server proxies `/api` requests to the backend at `http://localhost:8080`.

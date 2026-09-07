# Gulvisha Enterprises

Business platform foundation for Gulvisha Enterprises, with a public enquiry flow and protected admin operations dashboard.

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- PostgreSQL 16+

## Current features

- Public marketing site with enquiry form
- PostgreSQL-backed enquiry persistence
- JWT authentication at `/api/auth/login`
- Protected admin dashboard at `/admin`
- Admin enquiry filtering and status updates
- Protected internal follow-up notes for each enquiry
- Admin search, date filtering, sorting, pagination, CSV export, bulk status updates, and archiving

## Run the backend

Set the database environment variables if your local PostgreSQL setup differs from the defaults, then run:

```powershell
cd backend/gulvisha-backend
mvn spring-boot:run
```

The health endpoint is available at `http://localhost:8080/api/health`.

The admin API uses JWT. Authenticate at `POST /api/auth/login` with `{"username":"admin","password":"change-me"}` and send the returned token as `Authorization: Bearer <token>`.

Defaults are `admin` / `change-me`; configure `ADMIN_USERNAME` and `ADMIN_PASSWORD` before using this outside local development.

For multiple admins, set `ADMIN_USERS` as comma-separated `username:password:ROLE` entries. Supported roles are `VIEWER` (read-only), `MANAGER` (read and update), and `ADMIN` (full admin access). Example: `sales:secret:MANAGER,analyst:secret:VIEWER`.

New-enquiry email notifications are disabled by default. Enable them with `MAIL_ENABLED=true`, `MAIL_NOTIFICATION_TO`, `MAIL_FROM`, and the standard `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, and `SPRING_MAIL_PASSWORD` settings.

## Run the frontend

```powershell
cd frontend/gulvisha-frontend
npm.cmd install
npm.cmd run dev
```

Open the URL printed by Vite (normally `http://localhost:5173`). The page requests `/api/health`, which Vite forwards to the backend during development.

Use the `Admin` link in the public navigation to open the dashboard.

## Build

```powershell
cd backend/gulvisha-backend
mvn verify

cd ../../frontend/gulvisha-frontend
npm.cmd run build
```

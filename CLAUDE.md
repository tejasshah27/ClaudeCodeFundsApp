# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Requirements are in `AGENTS.md`. Implementation plan is in `PLAN.md`. Do not add features beyond what those files specify.

---

## Running Locally

```bash
# Terminal 1 — Backend (port 8080)
cd funds-dashboard-backend
./mvnw spring-boot:run

# Terminal 2 — Frontend (port 4200)
cd funds-dashboard-frontend
npm install
ng serve
```

Open `http://localhost:4200`. The Angular dev server proxies `/api/**` to `http://localhost:8080` via `proxy.conf.json`, eliminating any cross-origin issues.

## Build & Test

```bash
# Backend — run all tests
cd funds-dashboard-backend
./mvnw test

# Backend — run a single test class
./mvnw test -Dtest=FundControllerTest

# Frontend — build production bundle
cd funds-dashboard-frontend
ng build

# Frontend — run tests
ng test
```

---

## Architecture Overview

### Backend (`funds-dashboard-backend`) — Spring Boot version 3.5.10 + Maven + Java JDK version 21

- **No database.** All fund data lives in `MockDataService` (an in-memory `ArrayList<Fund>` initialized via `@PostConstruct`). Mutations persist for the duration of the server session only.
- **Session-based authentication.** `SecurityConfig` uses `InMemoryUserDetailsManager` with 3 hardcoded users and STATEFUL session management (CSRF off). `POST /api/auth/login` validates credentials, Spring Security creates the HTTP session, and the response body returns `{ role }`. The browser session cookie is sent automatically on subsequent requests.
- **Role-aware DTO projection.** `GET /api/funds` returns `FundSummaryDto[]` (5 fields) for EDITOR/READ_ONLY roles, or `FundDetailDto[]` (all 20 fields) for APPROVER. `GET /api/funds/{id}` always returns `FundDetailDto`.
- **Authorization in controllers.** Role checks are done by reading from `SecurityContextHolder` — no `@PreAuthorize`. Unauthorized actions return HTTP 403.

Key packages under `src/main/java/com/fundsdashboard/`:

| Package       | Responsibility                                                                               |
| ------------- | -------------------------------------------------------------------------------------------- |
| `model/`      | `Fund` POJO (20 fields), `FundStatus` enum, `UserRole` enum                                  |
| `service/`    | `MockDataService` (data store), `FundService` (DTO projection + business logic)              |
| `config/`     | `SecurityConfig` (session-based, hardcoded users, CSRF off)                                  |
| `controller/` | `AuthController` (`/api/auth/login`, `/api/auth/logout`), `FundController` (`/api/funds/**`) |
| `dto/`        | `LoginRequest/Response`, `FundSummaryDto`, `FundDetailDto`, `FundUpdateRequest`              |

### Frontend (`funds-dashboard-frontend`) — Angular v17+ standalone

- **Standalone components** with `@for` / `@if` control-flow syntax (not `*ngFor` / `*ngIf`).
- **No Angular Material** — all CSS is custom. Inter font (Google Fonts), CSS custom properties, smooth scroll.
- **HttpClient** configured with `withCredentials: true` so the session cookie is included on every request.
- **Routing:** `/login` (public), `/dashboard` (auth-guarded), `/` redirects to `/dashboard`.
- **Fund detail** opens as a full-screen overlay with a slide-in CSS animation (translateY + opacity, 300ms ease-out).

Key structure under `src/app/`:

| Path                            | Responsibility                                |
| ------------------------------- | --------------------------------------------- |
| `core/services/auth.service.ts` | Login/logout API calls, stores role in memory |
| `core/services/fund.service.ts` | Fund list and detail API calls                |
| `core/guards/auth.guard.ts`     | Redirects unauthenticated users to `/login`   |
| `features/login/`               | Login form component                          |
| `features/dashboard/`           | Fund table; role-aware column count           |
| `features/fund-detail/`         | Overlay modal; role-aware action buttons      |

---

## User Roles & Credentials

| Role      | Username   | Password   | Dashboard columns | Detail actions           |
| --------- | ---------- | ---------- | ----------------- | ------------------------ |
| Editor    | `editor`   | `password` | 5                 | Save / Submit / Close    |
| Approver  | `approver` | `password` | 20 (all)          | Approve / Reject / Close |
| Read Only | `readonly` | `password` | 5                 | Close only               |

## API Contract

| Method | Endpoint                  | Auth required | Notes                                      |
| ------ | ------------------------- | ------------- | ------------------------------------------ |
| POST   | `/api/auth/login`         | No            | Returns `{ role }`, sets session cookie    |
| POST   | `/api/auth/logout`        | Yes           | Invalidates session                        |
| GET    | `/api/funds`              | Yes           | Returns summary or full DTOs based on role |
| GET    | `/api/funds/{id}`         | Yes           | Always returns full 20-field DTO           |
| PUT    | `/api/funds/{id}`         | EDITOR only   | `{ action: "SAVE"\|"SUBMIT", ...fields }`  |
| POST   | `/api/funds/{id}/approve` | APPROVER only | Sets status → APPROVED                     |
| POST   | `/api/funds/{id}/reject`  | APPROVER only | Sets status → REJECTED                     |

## Fund Status Values

`DRAFT` → `SUBMITTED` (Editor submits) → `APPROVED` or `REJECTED` (Approver acts)

## Testing Focus

Backend integration tests are the priority (see `AGENTS.md` Technical Considerations §5):

- `AuthControllerTest` — valid logins for all 3 roles + invalid credentials → 401
- `FundControllerTest` — column counts per role, save/submit/approve/reject, 403 for unauthorized role actions

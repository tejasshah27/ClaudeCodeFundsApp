# Fund Dashboard — Implementation Plan

## Context

Greenfield project. The repo contains only AGENTS.md and CLAUDE.md with requirements. This plan covers the full build of a Fund Dashboard web app with Angular frontend and Spring Boot backend, supporting three user roles with role-based data visibility and fund management actions.

---

## User Credentials

| Role      | Username | Password |
| --------- | -------- | -------- |
| Editor    | editor   | password |
| Approver  | approver | password |
| Read Only | readonly | password |

---

## Fund Data Model

**Dashboard list (5 columns):** Fund Name, ISIN, Currency, NAV, Status

**Full detail (all 20 fields):** Fund Name, ISIN, Currency, NAV, Status + Asset Class, Fund Manager, Inception Date, Benchmark, Domicile, Risk Rating, AUM, Min Investment, Management Fee, Performance Fee, Distribution Frequency, Fund Type, Legal Structure, Bloomberg Ticker, Bloomberg ID

**Fund Status enum:** DRAFT, SUBMITTED, APPROVED, REJECTED
**25 dummy funds:** FUND-001 to FUND-025, statuses spread across all four values.

---

## Project Structure

```
Funds Dahsboard/
├── AGENTS.md
├── CLAUDE.md
├── PLAN.md
├── funds-dashboard-backend/      ← Spring Boot + Maven
│   ├── pom.xml
│   └── src/main/java/com/fundsdashboard/
│       ├── FundsDashboardApplication.java
│       ├── config/
│       │   └── SecurityConfig.java        (session-based, hardcoded users, CSRF off)
│       ├── controller/
│       │   ├── AuthController.java        POST /api/auth/login, POST /api/auth/logout
│       │   └── FundController.java        GET|PUT /api/funds, GET /api/funds/{id}, POST /api/funds/{id}/approve|reject
│       ├── dto/
│       │   ├── LoginRequest.java / LoginResponse.java
│       │   ├── FundSummaryDto.java        (5 fields for Editor/ReadOnly)
│       │   ├── FundDetailDto.java         (all 20 fields for Approver + fund-by-id)
│       │   └── FundUpdateRequest.java
│       ├── model/
│       │   ├── Fund.java                  (plain POJO, all 20 fields)
│       │   ├── FundStatus.java            (enum)
│       │   └── UserRole.java              (enum)
│       └── service/
│           ├── FundService.java
│           └── MockDataService.java        ← in-memory data store
│   └── src/test/java/com/fundsdashboard/
│       └── controller/
│           ├── AuthControllerTest.java
│           └── FundControllerTest.java
└── funds-dashboard-frontend/     ← Angular v17+ standalone
    ├── package.json
    ├── angular.json
    ├── proxy.conf.json             /api/** → http://localhost:8080
    └── src/
        ├── styles.css              (Inter font, global resets, CSS vars, smooth scroll)
        └── app/
            ├── app.config.ts       (provideRouter, provideHttpClient)
            ├── app.routes.ts
            ├── core/
            │   ├── guards/auth.guard.ts
            │   ├── models/fund.model.ts / user.model.ts
            │   └── services/auth.service.ts / fund.service.ts
            └── features/
                ├── login/          login.component.ts|html|css
                ├── dashboard/      dashboard.component.ts|html|css
                └── fund-detail/    fund-detail.component.ts|html|css
```

---

## Implementation Steps (ordered)

### Phase 1 — Backend Foundation

1. **Bootstrap Spring Boot project** — `pom.xml` with `spring-boot-starter-web`, `spring-boot-starter-security`, Lombok, `spring-boot-starter-test`
2. **Domain models** — `Fund.java` (20-field POJO), `FundStatus` enum, `UserRole` enum
3. **MockDataService** — `@Service`, `@PostConstruct initFunds()`, `ArrayList<Fund>` with 25 funds, exposes `getAll()`, `findById()`, `update()`
4. **FundService** — role-aware DTO projection (`FundSummaryDto` vs `FundDetailDto`), business logic for save/submit/approve/reject
5. **Session-based security** — `SecurityConfig` with `InMemoryUserDetailsManager` (3 hardcoded users), session management (STATEFUL), CSRF off, permit `/api/auth/login` and `/api/auth/logout`. Role is stored in the session via Spring Security; `FundController` reads it from `SecurityContextHolder`.
6. **Controllers** — `AuthController` (POST /api/auth/login → `{ role }`, POST /api/auth/logout), `FundController` (role from SecurityContext, 403 for unauthorized ops)

### Phase 2 — Backend Tests (must pass before frontend work)

8. **AuthControllerTest** — 4 tests: valid login for each role, invalid credentials → 401
9. **FundControllerTest** — 10 tests: column counts per role, save/submit/approve/reject, 403 for unauthorized role actions

### Phase 3 — Angular Bootstrap

10. **Create Angular project** — `ng new funds-dashboard-frontend --standalone --style=css`
11. **Global styles** — Inter font (Google Fonts), CSS custom properties, `scroll-behavior: smooth`
12. **Core models, AuthService, FundService** — `HttpClient` configured with `withCredentials: true` so session cookie is sent automatically

### Phase 4 — Angular Features

13. **LoginComponent** — centered card, username/password inputs, navigate to `/dashboard` on success, inline error on failure
14. **DashboardComponent** — fund table, role-aware columns (`@if role === 'APPROVER'` for extra columns), row click opens detail overlay, smooth scroll
15. **FundDetailComponent** — full-screen overlay, centered modal panel, slide-in CSS animation (translateY + opacity, 300ms ease-out), role-aware buttons: ReadOnly→Close, Editor→Save/Submit/Close, Approver→Approve/Reject/Close
16. **Navbar** — app title, role badge, logout button
17. **Routing + AuthGuard** — `/login` (public), `/dashboard` (guarded), `/` → redirect to `/dashboard`

### Phase 5 — UI Polish

18. Typography (Inter 400/500/600), sticky table header, row hover states, status badge colors (DRAFT=grey, SUBMITTED=blue, APPROVED=green, REJECTED=red), custom scrollbar, login card box-shadow

### Phase 6 — Wire Up

19. **Proxy config** — `proxy.conf.json` routing `/api/**` to `http://localhost:8080`, registered in `angular.json`
20. **End-to-end verification** — login as all 3 roles, verify column counts, test all action buttons, verify in-memory state mutations persist within session

---

## Key Design Decisions

- **Backend**: Spring Boot version 3.5.10 + Maven + Java JDK version 21
- **No database**: `MockDataService` holds a mutable `ArrayList<Fund>`; mutations (save/submit/approve/reject) update objects in place and persist for the session.
- **Session-based auth**: Spring Security manages an HTTP session. Login returns `{ role }` in the response body; the browser session cookie is set automatically. `FundController` reads the role from `SecurityContextHolder` — no `@PreAuthorize` needed.
- **DTO projection**: `FundSummaryDto` (5 fields) for Editor/ReadOnly list, `FundDetailDto` (20 fields) for Approver list and all role fund-by-id calls.
- **No Angular Material**: All CSS is custom for full design control.
- **Angular v17 standalone + control flow syntax**: Uses `@for`, `@if` blocks (not `*ngFor`/`*ngIf`).
- **Proxy config**: Angular dev proxy (`proxy.conf.json`) forwards `/api/**` to `http://localhost:8080`, eliminating any cross-origin issues without needing CORS configuration.

---

## API Contract

| Method | Endpoint                | Who can call  | Notes                                                     |
| ------ | ----------------------- | ------------- | --------------------------------------------------------- |
| POST   | /api/auth/login         | Public        | Returns `{ role }`, sets session cookie                   |
| POST   | /api/auth/logout        | Authenticated | Invalidates session                                       |
| GET    | /api/funds              | All roles     | Returns FundSummaryDto[] or FundDetailDto[] based on role |
| GET    | /api/funds/{id}         | All roles     | Always returns FundDetailDto                              |
| PUT    | /api/funds/{id}         | EDITOR only   | Body: `{ action: "SAVE"\|"SUBMIT", ...fields }`           |
| POST   | /api/funds/{id}/approve | APPROVER only | Sets status → APPROVED                                    |
| POST   | /api/funds/{id}/reject  | APPROVER only | Sets status → REJECTED                                    |

---

## Running Locally

```bash
# Terminal 1 — Backend (starts on :8080)
cd funds-dashboard-backend
./mvnw spring-boot:run

# Terminal 2 — Frontend (starts on :4200)
cd funds-dashboard-frontend
npm install
ng serve
```

Open browser at `http://localhost:4200`

---

## Verification Checklist

1. Login as `editor/password` → dashboard shows 5 columns, 25 funds
2. Login as `approver/password` → dashboard shows all 20 columns
3. Login as `readonly/password` → 5 columns, detail popup shows only Close
4. Editor: edit a fund field → Save → reopen → confirm change persisted
5. Editor: Submit → status changes to SUBMITTED
6. Approver: Approve/Reject → status changes accordingly
7. ReadOnly: PUT request → backend returns 403
8. Editor: call approve endpoint → backend returns 403
9. All 14 backend integration tests pass (`./mvnw test`)

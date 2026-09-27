# architecture.md — AuthShield 360 (AuthenticatorShield360)

> Owner: Technical Lead / Architect · Status: **Baseline v1.0** · Applies to: backend, frontend, database, devops
> Source of truth: `AuthShield360_DacTa_UseCase.docx` (SRS traceability) and
> `AuthShield360-MÔ-TẢ-CHỨC-NĂNG-HỆ-THỐNG-CHI-TIẾT.docx` (functional spec, UC-01..UC-17).

## 1. Purpose & scope

AuthShield 360 is a **simulated school portal** protected by a configurable identity platform.
It exists to demonstrate, measure and audit three authentication modes:

| Mode | Name | Factors |
|------|------|---------|
| **S1** | Password-only | `PASSWORD` |
| **S2** | Password + OTP | `PASSWORD` → `MOBILE_OTP` |
| **S3** | Password + Mobile OTP + Email OTP | `PASSWORD` → `MOBILE_OTP` → `EMAIL_OTP` |

The school domain (students, teachers, classes, assignments, submissions, exam results) is the
*protected asset*; the identity engine is the *control plane*.

**Out of scope (per SRS 1.2):** real student data, real email/SMS to real people, third-party
production services. Everything is lab-local and uses simulated accounts only (**BR-01**).

## 2. System context (C4 level 1)

```
        ┌──────────────┐        ┌────────────────┐        ┌───────────────────┐
        │  Student     │        │  Teacher       │        │  Administrator     │
        └──────┬───────┘        └───────┬────────┘        └─────────┬─────────┘
               │                        │                           │
               └────────────┬───────────┴───────────────┬───────────┘
                            ▼                           ▼
                  ┌───────────────────┐        ┌──────────────────┐
                  │  React SPA        │  HTTPS │  Audit / SIEM    │
                  │  (frontend/)      │        │  (CSV/JSON/CEF)  │
                  └─────────┬─────────┘        └────────▲─────────┘
                            │  REST /api/v1              │ export
                            ▼                            │
                  ┌──────────────────────────────────────┴─────────┐
                  │            Spring Boot API (backend/)           │
                  │  Auth engine · RBAC · School domain · Audit     │
                  └───────┬───────────────┬───────────────┬────────┘
                          │               │               │
                    ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼──────┐
                    │ MySQL 8   │   │ OTP chan. │   │ Mail (SMTP)│
                    │ (primary) │   │ (TOTP/app)│   │  Mailtrap  │
                    └───────────┘   └───────────┘   └────────────┘
```

## 3. Logical architecture (C4 level 2)

Layered, dependency-inward design:

```
presentation  →  controller  (REST, DTO in/out, @Valid)
                  │
application   →  service     (use-case orchestration, transactions)
                  │
domain        →  entity      (JPA aggregates), enums, domain rules
                  │
infrastructure→  repository  (Spring Data JPA), security filters, OTP/mail
                  adapters, file storage, audit sink
```

Rules:
1. Controllers never touch repositories directly.
2. Services own transactions (`@Transactional`) and audit emission.
3. Entities never leave the service boundary; DTOs are mapped explicitly.
4. Cross-cutting concerns (audit, correlation id, exception mapping) live in `common/`.

### 3.1 Backend package map

```
com.authshield360
├── config/            SecurityConfig, JacksonConfig, WebConfig, properties
├── common/            ApiResponse, exceptions, GlobalExceptionHandler,
│                      CorrelationIdFilter, PageResult
├── audit/             AuditEvent, AuditService, AuditLog entity/repo, export
├── security/          JwtService (HMAC), JwtAuthFilter, SessionService,
│                      SessionRecord, CurrentUser, RbacGuard
├── auth/              AuthController, AuthService, dto/, OtpService,
│                      LockoutService, CaptchaChallengeService, captcha/
├── user/              User, RoleType, UserRepository, UserService,
│                      admin controller, dto/
├── school/            StudentProfile, TeacherProfile, Classroom, Enrollment,
│                      Assignment, AssignmentSubmission, ExamResult + services
├── dashboard/         Role-specific aggregation endpoints
├── analytics/         Clickable role-dashboard charts + drill-down endpoints
└── BackendApplication
```

## 4. Identity & session architecture

### 4.1 Login state machine

```
                 ┌────────────┐
                 │  ANONYMOUS │
                 └─────┬──────┘
      POST /auth/login │ (username+password)
                       ▼
              ┌──────────────────┐   invalid / locked   ┌───────────┐
              │ PASSWORD_VERIFY  ├─────────────────────►│  REJECT   │
              └────────┬─────────┘                      └───────────┘
                 valid │
        S1 ───────────►│◄─────────── ISSUE_SESSION
                       │
        S2/S3          ▼
              ┌──────────────────┐  bad/expired  ┌───────────────────┐
              │ MOBILE_OTP_CHALL │──────────────►│ resend / restart   │
              └────────┬─────────┘               └───────────────────┘
                 valid │
        S2 ───────────►│◄─────────── ISSUE_SESSION
                       │
        S3             ▼
              ┌──────────────────┐
              │ EMAIL_OTP_CHALL  │
              └────────┬─────────┘
                 valid │
                       ▼
                 ISSUE_SESSION  ──► RBAC applied (UC-05)
```

A login produces a **ChallengeToken** (short-lived, purpose-scoped) between factors. Only after the
final factor does the system mint an **AccessToken** and persist a `session_record`.

### 4.2 Tokens

- **AccessToken**: compact HMAC-SHA256 signed token (`header.payload.signature`) — no external JWT
  library needed. Claims: `sub` (userId), `sid` (sessionId), `role`, `iat`, `exp`.
- **ChallengeToken**: same signing, `purpose=MOBILE_OTP|EMAIL_OTP`, `exp` = OTP window.
- **Server-side authority**: the `session_record` row is the source of truth. A syntactically valid
  token whose session is revoked/blacklisted is rejected → `401 SESSION_REPLAY_ATTEMPT` (UC-06, BR-08).

### 4.3 Logout / session invalidation (UC-06)

1. `POST /api/v1/auth/logout` → mark session `active=false, revoked_at=now`, add `sid` to denylist.
2. Client clears `sessionStorage`, `localStorage.auth_token`, and session cookies.
3. Client performs `window.location.replace("/login")` so the protected page leaves history.
4. Replaying the old token → `401` + `SESSION_REPLAY_ATTEMPT` audit event.

## 5. Authentication modes & configuration (UC-08)

A single `auth_config` row (singleton, id=1) drives runtime behaviour:

```
mode                 S1 | S2 | S3
otpType              TOTP | SMS_SIMULATED | EMAIL
otpLength            6
otpValiditySeconds   30..300
resendCooldownSec    60          (server-side throttle → 429)
maxResend            3           (then restart from step 1 → VĐ-07)
maxFailedAttempts    5           (BR-04)
lockoutDurationsSec  60,300,900  (exponential)
requireCaptchaAfter  3           (bot challenge when OTP requested repeatedly)
smtp*                host/port/user/password(encrypted)/from
```

Secrets (SMTP password, signing key) are **never** stored plaintext in repo files (**BR-10**);
they are read from env or encrypted at rest.

## 6. RBAC (UC-05, BR-05)

Authorization is enforced **server-side on every request**; hiding menu items is cosmetic only.

| Module | Student | Teacher | Administrator |
|---|---|---|---|
| Student profile | own record | records of own classes | full |
| Assignments | view, submit, resubmit* | create/edit/grade | view, manage |
| Submissions | own only | own classes | all |
| Exam results | own only | enter/edit for own classes | view/manage |
| User & role admin | – | – | full |
| Auth config | – | – | full |
| Audit log | – | – | view |

\* resubmission subject to assignment policy (see `use-cases.md` UC-A1..UC-A4).
Denied access → `403` + `PRIVILEGE_VIOLATION` audit event.

## 7. Audit architecture (UC-11, BR-06/BR-07)

Every auth/admin event is persisted as a structured row **and** renderable as JSON:

```json
{
  "timestamp": "...", "event_id": "...", "client_ip": "...", "user_agent": "...",
  "user_identifier": "...", "role": "...", "auth_factor": "...", "event_action": "...",
  "status": "...", "failure_reason": "...", "session_id": "..."
}
```

- Writes are synchronous inside the use-case transaction so the log appears in **< 5 s** (BR-06).
- Sensitive values (password, OTP, secret, raw tokens) are **never** logged (BR-02/BR-10).
- Export masks PII (`stu******@mailtrap.io`, `0912***678`), caps at 10,000 rows, names files
  `auth_logs_[MODE]_[YYYYMMDD]_[HHMMSS].[csv|json]`.
- Meta-logging: viewing/exporting logs itself writes `LOG_VIEW` / `LOG_EXPORT`.

## 8. School domain & assignment lifecycle

```
Assignment (DRAFT → PUBLISHED → CLOSED)
   dueAt, allowLate, lateCutoffAt, latePenaltyPct, allowResubmission, maxAttempts, maxScore
        │ 1..n
        ▼
AssignmentSubmission (attempt #n)
   student, file meta, submittedAt, onTime/late, score, feedback, gradedBy
```

Four student scenarios are first-class (see `use-cases.md`):
A1 submit on time · A2 submit late · A3 locked/no update · A4 update submitted file (versioned).

## 9. Cross-cutting concerns

| Concern | Mechanism |
|---|---|
| Validation | Bean Validation on request DTOs + service invariants |
| Errors | `GlobalExceptionHandler` → consistent `ApiResponse` envelope |
| Correlation | `X-Correlation-Id` filter, echoed in responses and audit rows |
| Concurrency | optimistic locking (`@Version`) on `Assignment`/`AssignmentSubmission` |
| Config | Spring profiles: `dev` (H2), `mysql` (MySQL primary), `test` |
| Time | UTC everywhere (`Instant`), UI localises |

## 10. Deployment view

- `frontend/` → static build served by Vite dev server (dev) / any static host (prod).
- `backend/` → Spring Boot fat jar; behind the SPA proxy in dev (`/api` → :8080).
- `db/` → MySQL 8 schema + seed; H2 auto-schema in dev.
- `docker-compose.yml` → mysql + backend + frontend for a reproducible lab (UC-14 reset).

## 11. Key decisions (ADR summary)

| # | Decision | Rationale |
|---|---|---|
| AD-1 | Server-side session registry + signed token | Enables true revocation/replay detection (BR-08) |
| AD-2 | Custom HMAC token instead of a JWT lib | Fewer deps, full control, sufficient for lab scope |
| AD-3 | Singleton `auth_config` row | Simple, atomic mode switch (UC-08) |
| AD-4 | OTP codes stored **hashed** | Defence in depth (BR-03/BR-10) |
| AD-5 | MySQL primary + H2 dev profile | Meets requirement, runs without local MySQL |
| AD-6 | Versioned submissions | Naturally satisfies A2/A4 without data loss |
| AD-7 | Audit written in-transaction | Guarantees < 5 s visibility (BR-06) |

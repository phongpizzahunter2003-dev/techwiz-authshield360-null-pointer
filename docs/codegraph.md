# codegraph.md — Code Graph, Dependencies & Ownership

> Owner: Architect · Status: **Baseline v1.0**
> A navigable map of modules, their dependencies, and the key call graphs.

## 1. Module dependency graph

```
                      ┌──────────────┐
                      │  frontend    │  (React SPA)
                      └──────┬───────┘
                             │ REST /api/v1 (JSON)
                             ▼
   ┌─────────────────────────────────────────────────────────┐
   │                     backend (Spring Boot)                │
   │                                                          │
   │  web/controller ──► service ──► repository ──► DB (MySQL/H2)
   │        │               │            │
   │        │               ├──► audit ──┘
   │        │               ├──► security (sessions, tokens)
   │        │               └──► integration (mail, otp channel, storage)
   │        ▼
   │  common (envelope, errors, filters)
   └─────────────────────────────────────────────────────────┘
```

Dependency direction: `controller → service → repository → domain`. `common`, `security`, `audit`
are cross-cutting and may be used by services but must not depend on controllers.

## 2. Backend class graph (key nodes)

### 2.1 Auth / session
```
AuthController
 ├─ AuthService
 │   ├─ UserRepository
 │   ├─ PasswordEncoder
 │   ├─ LockoutService ──────────► LoginAttemptRepository, UserRepository, AuditService
 │   ├─ OtpService ──────────────► OtpTokenRepository, MailGateway, AuditService
 │   ├─ CaptchaChallengeService ─► AuditService
 │   └─ SessionService ──────────► SessionRepository, JwtService, AuditService
 └─ DTOs (LoginRequest, LoginResponse, OtpVerifyRequest, ResendRequest)
JwtAuthFilter ──► JwtService, SessionService
SecurityConfig ──► JwtAuthFilter, RbacGuard
```

### 2.2 Audit
```
AuditService ──► AuditLogRepository, RequestContext (ip/ua/correlation)
AuditController (ADMIN) ──► AuditQueryService, MaskingUtil, ExportService(Csv/Json/Cef)
```

### 2.3 School domain
```
AssignmentController
 ├─ AssignmentService ──► AssignmentRepository, ClassroomRepository, AuditService
 └─ SubmissionService
      ├─ AssignmentSubmissionRepository
      ├─ FileStorageService
      ├─ SubmissionPolicy (on-time/late/locked/resubmit)  ◄── UC-A1..A4
      └─ AuditService
ExamResultService ──► ExamResultRepository, AuditService
DashboardService ──► per-role aggregation queries
```

### 2.4 Users / config
```
AdminUserController ──► UserService ──► UserRepository, PasswordEncoder, AuditService
AdminConfigController ──► ConfigService ──► AuthConfigRepository, CryptoService, MailGateway
```

## 3. Frontend component graph

```
App
 └─ AuthProvider
     └─ Router
        ├─ /login ── LoginPage ── StepPassword / StepMobileOtp / StepEmailOtp / LockoutTimer
        ├─ ProtectedRoute(role)
        │   ├─ AppShell ─ Sidebar, Topbar, Toaster
        │   ├─ StudentDashboard ─ AssignmentList ─ SubmissionPanel(A1..A4)
        │   ├─ TeacherDashboard  ─ AssignmentEditor ─ GradingTable
        │   └─ AdminDashboard    ─ UsersPage / ConfigPage / AuditLogViewer / ComparisonPage
        └─ ErrorPage(403/404/500)
```

Shared: `api/*` (axios modules) ← consumed by feature pages; `hooks/*` ← used by pages/components.

## 4. Key call graphs (sequence-level)

### 4.1 S2 login
```
FE:login → POST /auth/login
  → AuthService.login
      → LockoutService.assertNotLocked
      → PasswordEncoder.matches
      → AuthConfig.mode == S2 ⇒ OtpService.issue(MOBILE_OTP) → MailGateway/SmsSim
      → Audit(LOGIN_ATTEMPT, OTP_SENT)
      → return {OTP_REQUIRED, challengeToken}
FE:otp → POST /auth/otp/verify
  → AuthService.verifyOtp → OtpService.verify (hash, window, single-use)
      → ok ⇒ SessionService.issue → Audit(OTP_VERIFY_SUCCESS, LOGIN_SUCCESS) → {token}
      → bad ⇒ LockoutService.recordFailure → Audit(OTP_VERIFY_FAIL)
```

### 4.2 Submission (UC-A1..A4)
```
FE:submit → POST /assignments/{id}/submissions
  → SubmissionService.create
      → AssignmentRepository.find (owner/class check)
      → SubmissionPolicy.evaluate(assignment, now, attempts)
            ON_TIME | LATE | BLOCKED(SUBMISSION_LOCKED | LATE_NOT_ALLOWED |
                                   RESUBMISSION_NOT_ALLOWED | MAX_ATTEMPTS_REACHED)
      → FileStorageService.store
      → save attempt row
      → Audit(SUBMISSION_CREATE | SUBMISSION_UPDATE | SUBMISSION_BLOCKED)
```

## 5. Ownership (RACI)

| Area | Responsible | Accountable |
|---|---|---|
| Auth engine | Backend dev | Tech lead |
| RBAC/security | Security owner | Architect |
| Audit/export | Backend dev | BA |
| School domain | Backend dev | BA |
| FE design system | FE dev | FE lead |
| Dashboards | FE dev | FE lead |
| Docs | BA | Tech lead |
| DevOps | Ops | Architect |

## 6. Extension points

- New auth factor → implement `OtpChannel` interface, register in `OtpService`.
- New export format → implement `AuditExporter`.
- New role → matrix + `RoleType` + URL rules + FE route group (requires ADR).

# be-rules.md — Backend Rules (Spring Boot)

> Owner: Technical Lead · Stack: **Spring Boot 4.1.x · Java 17 · Spring Data JPA · Spring Security ·
> Bean Validation · MySQL 8 (primary) / H2 (dev)** · Status: **Baseline v1.0**

## 1. General principles

1. **Layered**: controller → service → repository. Controllers contain no business logic.
2. **Never trust the client** (BR-05). Validate every request; authorize every endpoint.
3. **DTO boundary**: entities never serialised directly; explicit mapping.
4. **Fail securely**: generic auth errors (no user/password enumeration — BA note UC-01).
5. **Everything auditable** (BR-07); secrets never logged (BR-02/BR-10).
6. **UTC** for all instants; `Instant` + `DATETIME(6)`.

## 2. Package conventions

```
com.authshield360.<module>
  <module>/            # e.g. auth, user, school, audit, security
    <X>Controller.java  # @RestController, /api/v1/..., returns ApiResponse
    <X>Service.java     # @Service @Transactional, orchestration
    <X>Repository.java  # extends JpaRepository
    domain/             # entities + enums
    dto/                # request/response records
    mapper/             # entity <-> dto
```
Class naming: `PascalCase`. Endpoint naming: `kebab-case`, plural resources, `/api/v1` prefix.

## 3. API contract

Uniform envelope:
```json
{ "success": true, "code": "OK", "message": "...", "data": { }, "timestamp": "...", "correlationId": "..." }
```
- Errors: `success=false`, non-null `code` (machine) + `message` (VI user text) + optional `fields`.
- Status codes: `200` ok, `201` created, `400` validation, `401` unauthenticated, `403` forbidden,
  `404` not found, `409` conflict (business state, e.g. locked submission), `429` throttled, `500` server.
- `POST /auth/login` returns either `{status:"AUTHENTICATED", token}` or `{status:"OTP_REQUIRED", challengeToken, factor, expiresIn}`.

## 4. Security implementation

| Concern | Rule |
|---|---|
| Password hashing | BCrypt strength **12** (BR-02) |
| OTP at rest | SHA-256 hash; compare constant-time |
| Token | HMAC-SHA256 signed, short-lived; `sid` claim ties to `session_record` |
| Session authority | DB row is truth; revoked/blacklisted ⇒ 401 (BR-08) |
| RBAC | `@PreAuthorize("hasRole('ADMIN')")` + URL rules; defense in depth |
| Lockout | 5 failures → exponential (BR-04); wrong OTP counts (VĐ-04) |
| Throttle | resend < 60 s ⇒ **429** without sending (UC-04) |
| Captcha | after N rapid OTP requests, require a challenge (UC-02/03) |
| Step-up | sensitive admin ops require fresh OTP (UC-17) |
| CORS | explicit allowed origins from config, never `*` with credentials |
| CSRF | disabled for token API; if cookies used, enable with SameSite=Strict |
| Secrets | env vars only; SMTP password AES-GCM encrypted at rest (BR-10) |

## 5. Validation

- `@Valid` on all request bodies; DTO records with constraints (`@NotBlank`, `@Size`, `@Email`, `@Pattern`).
- Business invariants checked in services, thrown as domain exceptions:
  `BusinessException(code, httpStatus)`.
- `GlobalExceptionHandler` maps: validation → 400 with `fields`; `BusinessException` → its status;
  `AccessDeniedException` → 403 + audit; auth failures → 401.

## 6. Transactions & concurrency

- Read methods `@Transactional(readOnly = true)`; mutations `@Transactional`.
- Audit rows written **in the same transaction** as the action where possible (BR-06 < 5 s).
- Optimistic locking (`@Version`) on `Assignment` and `AssignmentSubmission`.
- Lockout/counter updates use atomic DB update or row lock (`SELECT ... FOR UPDATE`).

## 7. Audit emission rules

- Central `AuditService.record(event)`; every event carries the standard schema (ba-rules §5).
- Never log: password, OTP code, MFA secret, raw token, SMTP password.
- Capture `client_ip` (X-Forwarded-For aware) and `user_agent` from the request context.
- Log meta-events for viewing/exporting logs (`LOG_VIEW`, `LOG_EXPORT`).

## 8. Business-rule → enforcement mapping

| BR | Where enforced |
|---|---|
| BR-01 | seed/validation only simulated data |
| BR-02 | `PasswordEncoder` + log sanitizer |
| BR-03 | `OtpService` validity window |
| BR-04 | `LockoutService` |
| BR-05 | `SecurityConfig` + `@PreAuthorize` + `RbacGuard` |
| BR-06 | synchronous audit write |
| BR-07 | `audit_logs` schema |
| BR-08 | `SessionService` revoke/ban |
| BR-09 | aggregation endpoints (UC-15) |
| BR-10 | config/properties + encryption |
| BR-11 | environment scope (docs) |
| BR-12 | persistent DB + seed |

## 9. Coding standards

- Java 17 features (records, switch expressions, `var` locally).
- No `System.out`; use SLF4J. No wildcard imports.
- Constructor injection only (no field `@Autowired`).
- Public methods documented where non-obvious; keep methods < ~40 lines.
- Unit tests for services (happy + business exceptions), slice tests for controllers.

## 10. Build & run

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # H2, no MySQL needed
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql    # MySQL 8
./mvnw clean verify                                        # tests + package
```
Profiles: `dev` (H2 + `ddl-auto=update`), `mysql` (`ddl-auto=validate` + `db/schema.sql`), `test`.

## 11. Definition of Done (backend)

Endpoint documented; RBAC enforced; validation + error envelope; audit events emitted; no secret
exposed; tests pass; matches the UC; no N+1 on list endpoints (fetch joins / DTO projections).

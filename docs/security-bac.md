# security-bac.md — Security & Broken Access Control (BAC) Controls

> Owner: Security owner / Architect · Status: **Baseline v1.0**
> Focus: BR-05 (RBAC server-side), BR-08 (session), BR-10 (secrets), and the authorized
> assessment scope (UC-12/UC-13, BR-11).

## 1. Authorized use scope (BR-11)

Testing is permitted **only** against the local lab environment of this project. Never against
real systems or real accounts. All data is simulated (BR-01).

## 2. Assets & trust boundaries

| Asset | Sensitivity |
|---|---|
| Credentials / password hash | Critical |
| MFA secret, OTP codes | Critical |
| Session tokens / cookies | Critical |
| SMTP credentials, signing key | Critical (secret, BR-10) |
| Student records, grades | Confidential / PII |
| Audit logs | Integrity-critical |

Trust boundary: untrusted **browser/network** → trusted **API**. Nothing from the client is trusted.

## 3. Broken Access Control model

### 3.1 Vertical (role) escalation
Controls:
- `@PreAuthorize` per endpoint (`hasRole('STUDENT'|'TEACHER'|'ADMIN')`).
- URL-level rules in `SecurityConfig` as a second layer.
- Service-level ownership checks (a Student can only read **own** rows).
- Denied request → `403` + `PRIVILEGE_VIOLATION` audit (user, role, requested_url, ip).
- Menu hiding is **cosmetic only**; never the control.

### 3.2 Horizontal escalation (IDOR)
- Every read/write on student data filters by the authenticated principal:
  `where student_id = :currentUserId` or `teacher owns classroom`.
- Never accept `userId` from the body for self-scoped operations — derive from token.
- Attempting another student's submission → `403`/`404` (no existence leak).

### 3.3 BAC test matrix (must all deny)

| # | Actor | Attempt | Expected |
|---|---|---|---|
| BAC-01 | STUDENT | `GET /api/v1/admin/users` | 403 + log |
| BAC-02 | STUDENT | `GET /api/v1/admin/audit-logs` | 403 + log |
| BAC-03 | STUDENT | `GET /api/v1/admin/config` | 403 + log |
| BAC-04 | TEACHER | `GET /api/v1/admin/users` | 403 + log |
| BAC-05 | TEACHER | grade a class they don't own | 403 + log |
| BAC-06 | STUDENT | submit to another student's assignment row | 403/404 |
| BAC-07 | STUDENT | `GET /api/v1/students/{otherId}/results` | 403/404 |
| BAC-08 | any | tampered token `role=ADMIN` | 401 (bad signature) |
| BAC-09 | any | replay revoked token | 401 + SESSION_REPLAY_ATTEMPT |

## 4. Authentication hardening

| Threat | Control |
|---|---|
| Credential stuffing | lockout after 5 fails, exponential backoff (BR-04) |
| User enumeration | generic error message (BA note UC-01) |
| OTP brute force | 6-digit, short TTL, wrong OTP counts to lockout (VĐ-04) |
| OTP replay | single-use token, newest-only valid (UC-04) |
| Resend spam | 60 s server throttle → 429; max 3 resends |
| Bot automation | captcha challenge after N rapid OTP requests |
| Session replay | `session_record` authoritative; revoke/denylist (BR-08) |
| Session fixation | new `sid` on successful login |
| Weak password | min length 8; hash bcrypt strength ≥ 12 (BR-02) |
| Token tampering | HMAC-SHA256 signature verified |
| Clock skew | OTP window ±1 step |

## 5. Secrets handling (BR-10)

- Signing key, SMTP password, DB password → **environment variables** only.
- `.env` and `application-*.local.properties` are git-ignored.
- SMTP password stored AES-GCM encrypted when persisted in `auth_config`.
- Logs sanitised: a scrubber strips password/otp/token/secret patterns before persistence.
- QA gate: secret scan (`git grep` patterns + entropy check) before release.

## 6. OWASP Top-10 alignment (selected)

| OWASP | Control here |
|---|---|
| A01 Broken Access Control | §3 of this file |
| A02 Cryptographic Failures | bcrypt, SHA-256 OTP hashing, AES-GCM secrets, HMAC token |
| A03 Injection | JPA parameter binding, Bean Validation, no dynamic SQL |
| A04 Insecure Design | threat model here + step-up for sensitive ops |
| A05 Misconfig | explicit CORS, no verbose errors, security headers |
| A07 Auth Failures | lockout, MFA, generic errors, session invalidation |
| A09 Logging Failures | structured audit + meta-logging, < 5 s visibility |

## 7. Security headers (set by API / static host)

`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`,
`Content-Security-Policy` (no inline script in prod), `Strict-Transport-Security` in HTTPS.

## 8. Incident evidence

Every denied/auth event is retained in `audit_logs` and exportable for the Test Matrix
(UC-11/UC-13). Evidence format: HTTP request/response (Burp/ZAP), matching log row, screenshot.

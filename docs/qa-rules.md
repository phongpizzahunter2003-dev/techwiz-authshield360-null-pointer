# qa-rules.md — Test Strategy, Cases & Evidence

> Owner: QA Lead · Status: **Baseline v1.0**
> Traceability: FR↔UC matrix (`use-cases.md` §9) and TC-01..TC-12 from the SRS, plus new
> assignment cases TC-A1..TC-A4.

## 1. Test levels

| Level | Scope | Tools |
|---|---|---|
| Unit | services, policies (lockout, OTP window, submission policy) | JUnit 5, Mockito |
| Integration | controllers + DB + security filters | Spring Boot Test, MockMvc |
| E2E/UI | login flows, dashboards, chart drill-down, submissions | manual + Playwright-ready |
| Security | BAC matrix (incl. `/analytics/*`), session replay, tampering | Burp/ZAP/DevTools (UC-12) |
| Performance | < 5 s log visibility, export cap | stopwatch, seed data |

## 2. Mandatory test cases (SRS §8) — must all be executed

| TC | Action | Mode | Expected | UC |
|---|---|---|---|---|
| TC-01 | Correct login, password only | S1 | success + log | UC-01 |
| TC-02 | Use leaked test password | S1 | succeeds (baseline weakness shown) | UC-01,12 |
| TC-03 | Correct password, no MFA factor | S2,S3 | denied, no session | UC-02,03 |
| TC-04 | Valid OTP | S2,S3 | success, log shows OTP factor | UC-02,03 |
| TC-05 | Wrong OTP | S2,S3 | denied, log "OTP không hợp lệ" | UC-02,03 |
| TC-06 | OTP submitted after expiry | S2,S3 | denied, log "OTP hết hạn" | UC-02,04 |
| TC-07 | Repeated wrong logins to threshold | S1–S3 | temp lock/limit/alert + lockout log | UC-10 |
| TC-08 | Student → Teacher/Admin resource | S1–S3 | denied + role-violation log | UC-05 |
| TC-09 | Teacher → Admin-only resource | S1–S3 | denied + role-violation log | UC-05 |
| TC-10 | Logout then reuse old session | S1–S3 | old session rejected | UC-06 |
| TC-11 | Email OTP valid/wrong/expired | S3 | valid→in; wrong/expired→denied+log | UC-03 |
| TC-12 | Account recovery / MFA reset (optional) | S2,S3 | correct flow + log + notice | UC-16 |

## 3. New assignment test cases (project extension)

| TC | Action | Expected | UC |
|---|---|---|---|
| TC-A1a | Student submits before `due_at` | `201`, status `ON_TIME`, log `SUBMISSION_CREATE` | UC-A1 |
| TC-A1b | Submit exactly at deadline | `201`, `ON_TIME` | UC-A1 |
| TC-A2a | Submit after due, `allow_late=true`, before cutoff | `201`, status `LATE`, penalty flagged | UC-A2 |
| TC-A2b | Submit after cutoff | `409 LATE_NOT_ALLOWED` + log | UC-A2 |
| TC-A2c | Submit after due, `allow_late=false` | `409 LATE_NOT_ALLOWED` + log | UC-A2 |
| TC-A3a | Update submission when assignment `CLOSED` | `409 SUBMISSION_LOCKED` + log `SUBMISSION_BLOCKED` | UC-A3 |
| TC-A3b | Update after grading / resubmission disabled | `409 RESUBMISSION_NOT_ALLOWED` | UC-A3 |
| TC-A4a | Resubmit while open, `allow_resubmission=true` | `201` new attempt, latest authoritative | UC-A4 |
| TC-A4b | Resubmit beyond `max_attempts` | `409 MAX_ATTEMPTS_REACHED` | UC-A4 |
| TC-A4c | Resubmit attempt history preserved | all attempts retrievable, ordered | UC-A4 |
| TC-A4d | Student A resubmits student B's row | `403/404` + log | UC-A4/BR-05 |

## 3.1 Dashboard analytics, drill-down & MFA ownership (added after review)

| TC | Action | Expected | UC |
|---|---|---|---|
| TC-D1 | Student opens dashboard | 3 role-specific charts render (assignment status PIE, attempts BAR, scores BAR) | UC-05 |
| TC-D2 | Student clicks a pie slice / legend chip | Navigates to `/student/assignments?bucket=<bucket>` pre-filtered to the matching items | UC-A1..A4 |
| TC-D3 | Click **Quay lại** on the filtered list | Returns to the previous page (`/student`) | C-04 |
| TC-D4 | Teacher opens dashboard | Charts differ from student/admin (submissions per assignment, grading progress, students per class) | UC-05 |
| TC-D5 | Teacher clicks a class column | Navigates to `/teacher/classes/:id` showing the roster and results | UC-05 |
| TC-D6 | Admin opens dashboard | 4 charts render (logins by mode, users by role, security events, 7-day line) | UC-11 |
| TC-D7 | Admin clicks a security-event bar | Navigates to `/admin/audit-logs?action=<ACTION>` with the action filter applied | UC-11 |
| TC-D8 | Admin clicks a chart legend chip on "events per day" | Audit log filtered to that day's `from`/`to` window | UC-11 |
| TC-D9 | Any role clicks a stat card / table row | Opens the matching detail page (`/admin/users/:id`, `/admin/audit-logs/:id`, `/student/assignments/:id`, `/teacher/assignments/:id`) | C-03 |
| TC-D10 | Non-admin calls `/api/v1/analytics/admin` | `403` + `PRIVILEGE_VIOLATION` log | BR-05 |
| TC-D11 | Student calls `/api/v1/analytics/teacher` | `403` + log | BR-05 |
| TC-M1 | User with `mfaEnabled && !mfaEnrolled` logs in (S2/S3) | UC-09 enrolment prompt appears after the password step | UC-09 |
| TC-M2 | User completes enrolment from the prompt | QR + secret shown to the **user**; confirming a valid TOTP sets `mfaEnrolled=true`, logs `MFA_ENROLL_SUCCESS` | UC-09 |
| TC-M3 | Admin resets a user's MFA | `mfaEnrolled=false`, old factor invalidated, `MFA_RESET` logged, user must re-enrol | UC-16 |
| TC-M4 | Verify admin has no "create QR for user" capability | Only self-scoped `/auth/mfa/enroll*` endpoints exist; admin path is reset-only | C-01 |
| TC-P1 | Admin applies S2 in bulk to STUDENT | Response `affected=2`; each student's `auth_mode_override=S2`, `mfaEnabled=true`; student login now returns `OTP_REQUIRED` | C-05 |
| TC-P2 | Admin applies INHERIT in bulk | Overrides cleared; effective mode falls back to the global `auth_config` (baseline login works) | C-05 |
| TC-P3 | Admin sets a single user's mode via the user editor | Only that account's override changes; other users unaffected | C-05 |
| TC-P4 | Non-admin calls `POST /admin/users/apply-auth-mode` | `403` + `PRIVILEGE_VIOLATION` | BR-05 |
| TC-P5 | Admin opens `/admin/system` | Shows JDBC product/URL, durability flag and per-table row counts read over the live connection | C-06 |
| TC-P6 | Scan the enrolment QR with an authenticator app | The `otpauth://` URI uses `%20` (never `+`) and includes `issuer`, `algorithm=SHA1`, `digits`, `period` | UC-09 |

## 4. Negative & abuse tests

- Wrong username vs wrong password → identical generic message (no enumeration).
- Resend within 60 s → `429 RESEND_OTP_THROTTLED` and **no** new OTP generated.
- 4th resend → `RESEND_OTP_LIMIT_EXCEEDED`, restart required.
- Tampered token → `401`; revoked token replay → `401 SESSION_REPLAY_ATTEMPT`.
- Oversized/again-type file upload → rejected, no partial row.
- Export > 10,000 rows → popup + only newest 10,000, masked PII.

## 5. Test data (BR-01 — simulated only)

| User | Role | Notes |
|---|---|---|
| `student01` | STUDENT | enrolled in `CS101` |
| `student02` | STUDENT | same class (IDOR tests) |
| `teacher01` | TEACHER | owns `CS101` |
| `admin01` | ADMIN | full control |
| `locked01` | STUDENT | used for lockout tests |

Emails use `@mailtrap.io`; phones are fake `09xx`. OTP surfaced via dev UI/console (VĐ-06).

## 6. Evidence rules (Identity Security Test Matrix — 7 columns)

Every executed case records: **Test ID · User/Role · Test Action · Expected Result · Actual
Result · Pass/Fail · Evidence**. Evidence = screenshot/HTTP request+response (Burp/ZAP) + matching
`audit_logs` row. For Fail: record cause, fix, rerun, keep **both** results.

Naming for exports: `auth_logs_[MODE]_[YYYYMMDD]_[HHMMSS].csv`.

## 7. Entry / exit criteria

**Entry:** build green, seed applied, config set for the target mode.
**Exit:** all TC-01..TC-12 + TC-A* executed; zero open S1/S2; NFR checks met
(< 5 s log visibility BR-06; ≥ 3 runs/mode BR-09); evidence attached; reset reproducible (UC-14).

## 8. Automation targets

- Unit: `SubmissionPolicy` (A1–A4 truth table), `OtpService` (window, single-use),
  `LockoutService` (threshold, exponential, VĐ-04).
- Integration: auth controller flows, RBAC denials, audit emission, export masking.
- Smoke script: seed → login each mode → submit → verify audit.

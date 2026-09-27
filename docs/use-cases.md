# use-cases.md — Detailed Use Cases

> Owner: Business Analyst · Status: **Baseline v1.0**
> UC-01..UC-17 are defined in the SRS/functional spec and summarised here for the team.
> UC-A1..UC-A4 are **new project extensions** (student assignment handling) requested by the client.
> Exact UI element IDs are in `fe-rules.md` §5; audit events in `ba-rules.md` §5.

## Part A — Identity & access use cases (UC-01..UC-17)

| UC | Name | Actor | Mode | Summary |
|---|---|---|---|---|
| UC-01 | Password-only login | all portal users | S1 | username+password → session; baseline |
| UC-02 | Password + OTP | all portal users | S2 | password then Mobile OTP |
| UC-03 | Multi-layer login | all portal users | S3 | password → Mobile OTP → Email OTP |
| UC-04 | Resend OTP | all portal users | S2,S3 | new code; 60 s throttle; max 3 then restart (VĐ-07) |
| UC-05 | Role-based access | Student/Teacher/Admin | S1–S3 | enforce matrix server-side (BR-05) |
| UC-06 | Logout & session end | all portal users | S1–S3 | revoke + clear client + history invalidation (BR-08) |
| UC-07 | User & role management | Admin | prep | create/edit users, assign roles, unique validation |
| UC-08 | Auth & MFA configuration | Admin | prep | choose S1/S2/S3, OTP params, SMTP; no plaintext secret |
| UC-09 | MFA enrollment | **any portal user (self-service)** | S2,S3 | after first password login the user scans the QR/secret and confirms a code; **admin never creates the QR** |
| UC-10 | Failed-login protection | system/Admin | S1–S3 | 5 fails → exponential lockout (BR-04, VĐ-04) |
| UC-11 | Auth log viewer | Admin, judges | S1–S3 | filter, paginate ≤50, masked export ≤10k |
| UC-12 | Authorized security assessment | team | S1–S3 | ethical testing, lab-only (BR-11) |
| UC-13 | Identity Security Test Matrix | team | S1–S3 | 7-column evidence matrix |
| UC-14 | Reset & durability | team/judges | ops | reset ≤60 s; config survives restart (BR-12) |
| UC-15 | Compare 3 modes | team | all | security vs usability, ≥3 runs/mode (BR-09) |
| UC-16 | Recovery / MFA reset (opt) | user/Admin | S2,S3 | recovery code/email, re-enroll |
| UC-17 | Step-up for admin (opt) | Admin | S2,S3 | fresh OTP before sensitive ops |

### Key flows (condensed)

**UC-01** B1 show form → B2 enter creds → B3 verify → B4 session+RBAC → B5 log → B6 dashboard.
A1 wrong ⇒ generic error+log; A2 threshold ⇒ UC-10; E1 locked ⇒ deny even if password right.

**UC-02** password ok → issue Mobile OTP → verify → session. A2 wrong OTP ⇒ deny+log; A3 expired ⇒ UC-04.
E1 skipping OTP ⇒ no full session.

**UC-03** password ok → Mobile OTP ok → Email OTP issued → verify → RBAC. Email wrong/expired ⇒
retry (≤3) or restart (VĐ-07); SMTP error ⇒ resend allowed + test note.

**UC-04** invalidate old code, issue new, reset countdown; <60 s ⇒ 429; >3 resends ⇒ restart.

**UC-06** revoke server session + denylist; clear `sessionStorage`/`localStorage`/cookies;
`window.location.replace('/login')`; replay old token ⇒ 401 + `SESSION_REPLAY_ATTEMPT`.

**UC-10** count failures/user/window → threshold ⇒ lock (1→5→15 min) → log; correct password during
lock still denied; release ⇒ `LOCKOUT_RELEASED`.

### Who owns MFA enrollment and reset? (UC-08 / UC-09 / UC-16) — **binding clarification**

| Question | Answer from the source specs |
|---|---|
| Who creates the QR code? | **The user themselves.** UC-09 *Tác nhân chính* = "Người dùng portal"; §9.1 = "Trang Đăng ký MFA (hiển thị sau khi đăng nhập mật khẩu thành công lần đầu)". Every role (Student / Teacher / Administrator) enrols its **own** second factor. |
| What does the Admin do? | UC-08: enables/configures the authentication mode, OTP parameters and SMTP at platform level (Đội thi / Administrator). The admin **does not** enrol MFA on behalf of a user and never sees the user's secret. |
| Can MFA be reset? | UC-16 (optional): *Tác nhân chính* = "Người dùng portal; Administrator" — the system **or** an admin may reset the factor, after which the user must re-enrol (UC-09). Reset is logged as `MFA_RESET`. |
| When is enrolment prompted? | UC-09 precondition "MFA đã được bật; người dùng chưa đăng ký yếu tố" + trigger "Lần đăng nhập đầu tiên sau khi bật MFA" ⇒ after a successful password login, if `mfaEnabled && !mfaEnrolled`, the portal prompts the user to enrol (implemented as a post-login modal, dismissible for the session; strict blocking can be enabled by policy). |

**Implementation mapping:** `POST /api/v1/auth/mfa/enroll` (start) and
`POST /api/v1/auth/mfa/enroll/confirm` (confirm) are self-scoped and require only an authenticated
session — no admin role. `POST /api/v1/admin/users/{id}/reset-mfa` is the admin reset path.

## Part B — NEW: Student assignment use cases (project extension)

These four cases are added per the client's explicit request. Scope: `Assignment` +
`AssignmentSubmission` (see `database.md` §3.6–3.7).

---

### UC-A1 — Submit an assignment on time

- **Actor:** Student · **Mode:** S1–S3 · **Traceability:** FR i, BR-05, BR-07
- **Preconditions:** logged in as STUDENT; enrolled in the assignment's classroom; assignment
  `PUBLISHED`; `now ≤ due_at`.
- **Trigger:** student selects a file and clicks **Nộp bài**.
- **Main flow:**
  B1. System loads assignment and policy. B2. Student uploads a file (type/size validated).
  B3. System records `submitted_at`; since `now ≤ due_at` → `submission_status = ON_TIME`.
  B4. Attempt row created (attempt #1). B5. Log `SUBMISSION_CREATE` (status SUCCESS).
  B6. UI shows "Đã nộp đúng hạn" with timestamp and download link.
- **Alternates/Exceptions:**
  A1 (B2): invalid file type/size → reject, keep form.
  A2 (B1): student not enrolled → `403` + `PRIVILEGE_VIOLATION`.
  E1 (B3): assignment `CLOSED` → `409 SUBMISSION_LOCKED` (see UC-A3).
- **Postcondition:** exactly one ON_TIME attempt exists; teacher can see it.
- **Rules:** BR-05, BR-07 · **TC:** TC-A1a, TC-A1b

---

### UC-A2 — Submit an assignment late

- **Actor:** Student · **Mode:** S1–S3 · **Traceability:** FR i, BR-05, BR-07
- **Preconditions:** as UC-A1; `now > due_at`.
- **Trigger:** student submits after the deadline.
- **Main flow:**
  B1. System evaluates policy: `allow_late` and `now ≤ late_cutoff_at`.
  B2. Accept → `submission_status = LATE`, `late_penalty_pct` applied to grading context.
  B3. Store attempt; UI shows "Đã nộp muộn (bị trừ {penalty}%)" with a warning badge.
  B4. Log `SUBMISSION_CREATE` with `failure_reason` empty but `detail={"late":true}`.
- **Alternates/Exceptions:**
  A1 (B1): `allow_late=false` → `409 LATE_NOT_ALLOWED` + log.
  A2 (B1): `now > late_cutoff_at` → `409 LATE_NOT_ALLOWED` + log.
- **Postcondition:** a LATE attempt exists, clearly flagged for the teacher.
- **Rules:** BR-05, BR-07 · **TC:** TC-A2a, TC-A2b, TC-A2c

---

### UC-A3 — Cannot update homework when submission is locked

- **Actor:** Student · **Mode:** S1–S3 · **Traceability:** FR i, BR-05, BR-07
- **Preconditions:** student already has a submission; the assignment is one of:
  `status = CLOSED`, or already graded, or `allow_resubmission = false`, or
  attempts = `max_attempts`.
- **Trigger:** student attempts to replace/update the submitted file.
- **Main flow:**
  B1. Student clicks **Cập nhật bài nộp**. B2. System evaluates the lock policy (server-side).
  B3. Locked → `409` with a specific code: `SUBMISSION_LOCKED` | `RESUBMISSION_NOT_ALLOWED` |
  `MAX_ATTEMPTS_REACHED`. B4. UI shows the reason and disables the update control.
  B5. Log `SUBMISSION_BLOCKED` (FAILURE, matching `failure_reason`).
- **Alternates/Exceptions:**
  A1 (B2): assignment open and resubmission allowed → proceed to UC-A4.
- **Postcondition:** no new attempt created; original submission unchanged; evidence logged.
- **Rules:** BR-05, BR-07 · **TC:** TC-A3a, TC-A3b

---

### UC-A4 — Submit homework and be able to update the submitted file

- **Actor:** Student · **Mode:** S1–S3 · **Traceability:** FR i, BR-05, BR-07
- **Preconditions:** prior submission exists; assignment `PUBLISHED`; `allow_resubmission = true`;
  attempts < `max_attempts`; not graded (or regrade permitted); within allowed time window.
- **Trigger:** student uploads a replacement file.
- **Main flow:**
  B1. System validates policy (same window as UC-A1/A2 for the new time).
  B2. New attempt row: `attempt_number = previous + 1`; previous attempt retained (history).
  B3. Latest attempt becomes authoritative for grading; its `submission_status` recomputed
  (ON_TIME/LATE at resubmit time).
  B4. UI shows attempt history ("Lần 1", "Lần 2", …) with the newest marked "Đang dùng".
  B5. Log `SUBMISSION_UPDATE` (SUCCESS) with attempt number.
- **Alternates/Exceptions:**
  A1 (B1): `allow_resubmission=false` or graded → `409 RESUBMISSION_NOT_ALLOWED` (UC-A3).
  A2 (B1): attempts = `max_attempts` → `409 MAX_ATTEMPTS_REACHED` (UC-A3).
  A3 (B1): after cutoff → `409 LATE_NOT_ALLOWED`.
  E1 (B2): another student's row targeted → `403/404` + `PRIVILEGE_VIOLATION`.
- **Postcondition:** ≥2 ordered attempts; newest is authoritative; all retained.
- **Rules:** BR-05, BR-07 · **TC:** TC-A4a, TC-A4b, TC-A4c, TC-A4d

---

### Submission policy truth table (implementation contract)

| now vs due | allow_late | vs cutoff | allow_resub | attempts | graded | Result |
|---|---|---|---|---|---|---|
| ≤ due | – | – | – | 0 | – | `ON_TIME` (create) |
| > due | true | ≤ cutoff | – | 0 | – | `LATE` (create) |
| > due | false | – | – | 0 | – | `409 LATE_NOT_ALLOWED` |
| > due | true | > cutoff | – | 0 | – | `409 LATE_NOT_ALLOWED` |
| any | – | – | – | ≥1, =max | – | `409 MAX_ATTEMPTS_REACHED` |
| any | – | – | false | ≥1 | – | `409 RESUBMISSION_NOT_ALLOWED` |
| any | – | – | true | ≥1, <max | yes | `409 RESUBMISSION_NOT_ALLOWED` |
| any | – | – | true | ≥1, <max | no | new attempt (`ON_TIME`/`LATE` by time) |
| any | – | – | any | any | any, assignment `CLOSED` | `409 SUBMISSION_LOCKED` |

## Part C — Traceability (FR ↔ UC)

| FR | Requirement | UC |
|---|---|---|
| i | Simulated school portal | UC-05, UC-07, UC-A1..A4 |
| ii | User & role management | UC-07, UC-05 |
| iii | Password-only baseline | UC-01, UC-15 |
| iv | Safe password storage | UC-01 (BR-02) |
| v | MFA configuration | UC-08, UC-09 |
| vi | MFA testing | UC-02, UC-03, UC-04, UC-13 |
| vii | Role-based access control | UC-05 |
| viii | Brute-force protection | UC-10 |
| ix | Session security | UC-06 |
| x | Auth log & monitoring | UC-11 |
| xi | Controlled ethical hacking | UC-12 |
| xii | Identity Security Test Matrix | UC-13 |
| xiii | Config reset | UC-14 |
| opt | Email step-up, recovery, admin step-up, notifications, log filtering | UC-03, UC-16, UC-17, UC-09(A3), UC-11(B3) |

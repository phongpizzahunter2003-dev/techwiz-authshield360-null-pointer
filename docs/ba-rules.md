# ba-rules.md — Business Analysis Rules & Decisions

> Owner: Business Analyst · Status: **Baseline v1.0**
> Source: `AuthShield360_DacTa_UseCase.docx` §3 (BR-01..BR-12), §10 (VĐ-01..VĐ-08) and the
> functional spec. This file is the **contract** between BA, dev, QA and ops.

## 1. Business rules (as required by the SRS)

| Code | Rule | Source | Implementation anchor |
|---|---|---|---|
| **BR-01** | Only simulated accounts/data/email/phone (or own). No real student data. | SRS 1.4/1.5 | seed data uses `@mailtrap.io`, `09xx` fakes |
| **BR-02** | Passwords hashed by the platform; never shown or logged in clear. | FR iv | BCrypt strength ≥ 12 |
| **BR-03** | OTP valid only within configured window; wrong/expired rejected + logged. | FR vi | `OtpService`, `otp_validity_seconds` |
| **BR-04** | Measurable failed-login threshold (e.g. 5) triggers temporary lock/rate-limit/delay/alert. | FR viii | `LockoutService` |
| **BR-05** | Each role only reaches permitted functions per access matrix; enforced **server-side**. | FR ii, vii | `RbacGuard`, `@PreAuthorize` |
| **BR-06** | New auth event appears on the monitoring UI within **5 seconds**. | NFR Perf | synchronous audit write |
| **BR-07** | Audit record = time, user, role, factor, action, source, session, result. | NFR Audit | `audit_log` schema |
| **BR-08** | Session invalidated after logout/timeout; not reusable. | FR ix | `session_record`, denylist |
| **BR-09** | Login time per mode = mean of ≥ 3 runs. | NFR Perf | comparison report (UC-15) |
| **BR-10** | Secrets (password, MFA secret, recovery code, SMTP creds, API key) never in repo/public files. | SRS 1.5 | env vars / encryption |
| **BR-11** | Security testing only against the project environment. | FR xi | lab-only scope |
| **BR-12** | Accounts, roles, MFA config and audit log survive restart. | NFR Rel | persistent DB |

## 2. Resolutions of open issues (VĐ) — **binding decisions**

| Code | Issue | Decision for this project |
|---|---|---|
| **VĐ-01** | Email OTP "where supported" vs part of S3 main flow. | **Treat as required.** S3 is fully implemented so the 3-mode comparison is complete. Assumption recorded in README. |
| **VĐ-02** | S3 called "step-up" but is sequential login. | Documented as **sequential 3-factor login**. A true **step-up** is additionally implemented for sensitive admin actions (UC-17). |
| **VĐ-03** | Mobile OTP and Email OTP are both "something you have". | Recorded in analysis: S3 adds a **layer**, not a **factor type**. |
| **VĐ-04** | Does a wrong OTP count toward lockout? | **Yes.** Wrong OTP increments the same failure counter → protects against OTP brute-force. Applied in UC-10 and Test Matrix. |
| **VĐ-05** | Undefined "login time". | Login time = from clicking **Đăng nhập** until the role dashboard renders (includes user time to fetch OTP). |
| **VĐ-06** | SMS to a simulated number may cost money. | Use an **authenticator (TOTP)** or a **simulated SMS channel** (code surfaced in the dev UI/console). No paid SMS. |
| **VĐ-07** | OTP-failure behaviour differs S2 vs S3. | Per mode: **S2** allows resend up to 3× then restart; **S3** same for Mobile, and Email resend up to 3× then restart from step 1. |
| **VĐ-08** | "Provided portal" ambiguity. | Self-hosted lab portal (this repo); no third-party dependency. |

## 3. Access matrix (BA proposal — confirmed)

| Module / function | Student | Teacher | Administrator |
|---|---|---|---|
| Student profile | own only | own-class students | full |
| Assignments | view + submit own | create/edit/grade | view/manage |
| Submissions | own only | own classes | all |
| Exam results | own only | enter/edit own classes | view/manage |
| User & role admin | ✗ | ✗ | ✓ |
| Auth/MFA config | ✗ | ✗ | ✓ |
| Audit log | ✗ | ✗ | ✓ (view) |
| Security comparison (S1/S2/S3) | ✗ | ✗ | ✓ |

## 4. Message catalogue (canonical, Vietnamese UI)

All user-facing messages are defined centrally (FE `src/i18n/messages.js`, BE `Msg` constants).
Selected mandatory strings:

| Key | Text |
|---|---|
| `LOGIN_INVALID` | Tên đăng nhập hoặc mật khẩu không chính xác. Vui lòng thử lại. |
| `LOCKOUT_ACTIVE` | Tài khoản của bạn đã bị tạm khóa do nhập sai quá nhiều lần. Vui lòng thử lại sau {mm:ss}. |
| `OTP_INVALID` | Mã xác minh không chính xác. Vui lòng kiểm tra lại. |
| `OTP_EXPIRED` | Mã xác minh đã hết hạn. Vui lòng yêu cầu gửi lại mã mới. |
| `OTP_RESENT` | Mã xác minh mới đã được gửi thành công. Vui lòng kiểm tra. |
| `RESEND_TOO_SOON` | Yêu cầu quá thường xuyên. Vui lòng đợi hết thời gian chờ. |
| `RESEND_LIMIT` | Bạn đã yêu cầu gửi lại mã quá nhiều lần. Vui lòng bắt đầu lại quy trình đăng nhập. |
| `FORBIDDEN` | Bạn không có quyền truy cập vào tài nguyên này. Hành vi vi phạm đã được ghi nhận. |
| `SESSION_EXPIRED` | Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại. |
| `LOGOUT_OK` | Bạn đã đăng xuất thành công. |
| `EXPORT_LIMIT` | Dữ liệu tìm kiếm vượt quá 10,000 bản ghi (Hiện có {X} bản ghi). Hệ thống sẽ tự động xuất 10,000 bản ghi mới nhất... |

## 5. Event / audit catalogue

**Event actions:** `LOGIN_ATTEMPT, LOGIN_FAIL, LOGIN_SUCCESS, LOCKOUT_TRIGGERED, LOCKOUT_RELEASED,
OTP_SENT, OTP_VERIFY_SUCCESS, OTP_VERIFY_FAIL, OTP_EXPIRED, RESEND_OTP_REQUEST, RESEND_OTP_SUCCESS,
RESEND_OTP_LIMIT_EXCEEDED, RESEND_OTP_THROTTLED, EMAIL_OTP_SENT, EMAIL_OTP_FAILED, EMAIL_OTP_EXPIRED,
MFA_ENROLL_SUCCESS, MFA_ENROLL_FAIL, LOGOUT, SESSION_EXPIRED, SESSION_REPLAY_ATTEMPT,
PRIVILEGE_VIOLATION, USER_CREATE, USER_UPDATE, USER_DELETE, ROLE_ASSIGN, CONFIG_CHANGE,
LOG_VIEW, LOG_EXPORT, ACCOUNT_RECOVERY_REQUEST, MFA_RESET, STEP_UP_AUTH_SUCCESS, STEP_UP_AUTH_FAIL,
ASSIGNMENT_CREATE, ASSIGNMENT_UPDATE, ASSIGNMENT_CLOSE, SUBMISSION_CREATE, SUBMISSION_UPDATE,
SUBMISSION_BLOCKED, SUBMISSION_GRADE, EXAM_RESULT_UPSERT`.

**Failure reasons:** `INVALID_CREDENTIALS, INVALID_OTP, EXPIRED_OTP, ACCESS_DENIED,
EXCEEDED_MAX_ATTEMPTS, MAX_RESEND_EXCEEDED, TOO_MANY_REQUESTS, INVALID_SESSION, ACCOUNT_LOCKED,
SUBMISSION_LOCKED, LATE_NOT_ALLOWED, RESUBMISSION_NOT_ALLOWED, MAX_ATTEMPTS_REACHED, SMTP_ERROR`.

## 6. Definition of Done (business)

A use case is Done when: (1) happy path + all alternative/exception flows behave as specified;
(2) every mandated audit event is emitted with the correct fields; (3) RBAC is enforced server-side;
(4) test cases exist with the 7 columns and evidence; (5) messages match the catalogue;
(6) no secret appears in code, config-in-repo, or logs.

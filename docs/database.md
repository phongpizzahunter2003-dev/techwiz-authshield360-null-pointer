# database.md — Data Model & Persistence

> Owner: Architect / DBA · Status: **Baseline v1.0**
> Primary engine: **MySQL 8** (profile `mysql`). Dev/test: **H2** (profile `dev`, MySQL-compat mode).
> Naming: `snake_case`, tables plural, FK columns `<entity>_id`, timestamps UTC (`Instant`).

## 1. Conventions

- PK: `BIGINT AUTO_INCREMENT` (`IDENTITY`).
- Every business table: `created_at`, `updated_at`; mutable aggregates add `version` (optimistic lock).
- Enums stored as `VARCHAR` with `CHECK` where useful (portable to H2).
- No secret stored in clear: passwords → bcrypt hash; OTP → SHA-256 hash; SMTP password → AES-GCM.
- Reference data seeded idempotently (`db/seed.sql` / `DataSeeder`).

## 2. Entity relationship (logical)

```
users 1───1 student_profiles
users 1───1 teacher_profiles
users 1───n sessions
users 1───n audit_logs (by user_identifier, not FK — logs survive user deletion)
users(teacher) 1───n classrooms
classrooms 1───n enrollments n───1 users(student)
classrooms 1───n assignments
assignments 1───n assignment_submissions n───1 users(student)
users(student) 1───n exam_results
auth_config (singleton)
otp_tokens  (transient, keyed by user_identifier + factor)
```

## 3. Tables

### 3.1 `users`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| username | VARCHAR(50) UNIQUE NOT NULL | max 50 (UC-07) |
| email | VARCHAR(255) UNIQUE NOT NULL | validated format |
| phone | VARCHAR(20) | simulated OTP channel |
| password_hash | VARCHAR(100) NOT NULL | bcrypt, strength ≥ 12 (BR-02) |
| full_name | VARCHAR(120) | |
| role | VARCHAR(20) NOT NULL | `STUDENT\|TEACHER\|ADMIN` |
| status | VARCHAR(20) NOT NULL | `ACTIVE\|LOCKED\|DISABLED` |
| mfa_enabled | BOOLEAN NOT NULL DEFAULT FALSE | |
| mfa_enrolled | BOOLEAN NOT NULL DEFAULT FALSE | |
| mfa_secret_enc | VARCHAR(255) | encrypted TOTP secret (BR-10) |
| failed_attempts | INT NOT NULL DEFAULT 0 | lockout counter (BR-04, VĐ-04) |
| lockout_level | INT NOT NULL DEFAULT 0 | exponential step index |
| locked_until | DATETIME(6) | null when unlocked |
| created_at / updated_at | DATETIME(6) | |

Indexes: `uq_users_username`, `uq_users_email`, `ix_users_role`.

### 3.2 `student_profiles`
`id, user_id FK UNIQUE → users, student_code UNIQUE, class_name, date_of_birth, guardian_contact, created_at, updated_at`

### 3.3 `teacher_profiles`
`id, user_id FK UNIQUE → users, employee_code UNIQUE, department, created_at, updated_at`

### 3.4 `classrooms`
`id, code UNIQUE, name, description, teacher_id FK → users, created_at, updated_at`

### 3.5 `enrollments`
`id, classroom_id FK, student_id FK (users), created_at` — unique (`classroom_id`,`student_id`).

### 3.6 `assignments`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| title | VARCHAR(200) | |
| description | TEXT | |
| classroom_id | FK → classrooms | |
| created_by | FK → users(teacher) | |
| due_at | DATETIME(6) | deadline for on-time (A1) |
| allow_late | BOOLEAN | A2 |
| late_cutoff_at | DATETIME(6) | after this, late is refused |
| late_penalty_pct | INT | e.g. 10 |
| allow_resubmission | BOOLEAN | A4 |
| max_attempts | INT | A4 cap |
| max_score | INT | default 100 |
| status | VARCHAR(20) | `DRAFT\|PUBLISHED\|CLOSED` (A3 lock) |
| version | BIGINT | optimistic lock |
| created_at / updated_at | DATETIME(6) | |

Indexes: `ix_assignments_classroom`, `ix_assignments_status_due`.

### 3.7 `assignment_submissions`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| assignment_id | FK | |
| student_id | FK → users | |
| attempt_number | INT | versioning (A4) |
| original_name | VARCHAR(255) | |
| stored_name | VARCHAR(255) | on-disk name |
| content_type | VARCHAR(120) | |
| size_bytes | BIGINT | |
| submitted_at | DATETIME(6) | |
| submission_status | VARCHAR(20) | `ON_TIME\|LATE` |
| score | INT NULL | |
| feedback | TEXT NULL | |
| graded_by | FK → users NULL | |
| graded_at | DATETIME(6) NULL | |
| version | BIGINT | |
| created_at / updated_at | DATETIME(6) | |

Unique (`assignment_id`,`student_id`,`attempt_number`). Latest attempt = max `attempt_number`.

### 3.8 `exam_results`
`id, student_id FK, subject, exam_name, score, max_score, exam_date, recorded_by FK, created_at, updated_at`

### 3.9 `audit_logs`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| event_time | DATETIME(6) NOT NULL | indexed (DESC) |
| event_id | VARCHAR(36) | UUID per event |
| event_action | VARCHAR(40) NOT NULL | see ba-rules §5 |
| status | VARCHAR(10) | `SUCCESS\|FAILURE` |
| auth_factor | VARCHAR(20) | `PASSWORD\|MOBILE_OTP\|EMAIL_OTP\|-` |
| user_identifier | VARCHAR(120) | indexed |
| role | VARCHAR(20) | |
| client_ip | VARCHAR(45) | indexed |
| user_agent | VARCHAR(512) | |
| requested_url | VARCHAR(512) | |
| session_id | VARCHAR(64) | indexed |
| failure_reason | VARCHAR(40) | |
| correlation_id | VARCHAR(36) | |
| detail | TEXT | sanitised JSON (never secrets) |

Indexes: `ix_audit_event_time`, `ix_audit_user`, `ix_audit_action`, `ix_audit_status`, `ix_audit_ip`.
Retention/partition note: for large volumes partition monthly by `event_time`; export capped 10,000.

### 3.10 `sessions`
`id, session_id UNIQUE(64), user_id FK, role, issued_at, expires_at, last_seen_at, client_ip, user_agent, active BOOLEAN, revoked_at, revoke_reason`
Index: `ix_sessions_user_active`, `ix_sessions_expires`.

### 3.11 `otp_tokens`
`id, user_identifier, factor, code_hash (SHA-256), issued_at, expires_at, consumed BOOLEAN, attempt_count, resend_count, last_sent_at, session_id NULL`
Index: `ix_otp_user_factor`.
Only the **latest unconsumed** token per (user, factor) is valid; issuing a new one invalidates the old (UC-04).

### 3.12 `auth_config` (singleton, id = 1)
`id, mode, otp_type, otp_length, otp_validity_seconds, resend_cooldown_seconds, max_resend,
max_failed_attempts, lockout_durations_seconds, require_captcha_after, smtp_host, smtp_port,
smtp_username, smtp_password_enc, smtp_from, updated_by, updated_at`

### 3.13 `login_attempts` (fast lockout window queries)
`id, user_identifier, client_ip, occurred_at, success, reason`
Index: `ix_attempts_user_time`. Query: failures in the configured window.

## 4. Constraints & invariants

1. A user has exactly one role (matrix is fixed).
2. `assignment_submissions.attempt_number` is monotonic per (assignment, student).
3. Only one `auth_config` row; updates are versioned by `updated_at`.
4. `session_record` is authoritative for token validity (BR-08).
5. Deleting a user does **not** delete audit rows (compliance).
6. `late_cutoff_at >= due_at` when `allow_late = true`.

## 5. Profiles & DDL delivery

- `dev` (H2): `spring.jpa.hibernate.ddl-auto=update`, seeded by `DataSeeder`.
- `mysql` (MySQL 8): schema in `db/schema.sql`, seed in `db/seed.sql`, `ddl-auto=update` on first provision then `validate`.
- Reset (UC-14): `db/reset.sql` + `docker compose down -v && up -d` (30–60 s, BR-12 persistence check).

## 6. Migration path

Baseline DDL ships as SQL. Future schema changes follow additive migrations (Flyway-ready naming
`V__desc.sql`); destructive changes require an ADR in `architecture.md` §11 and BA sign-off.

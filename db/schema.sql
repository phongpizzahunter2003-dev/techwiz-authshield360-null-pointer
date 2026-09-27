-- ============================================================================
-- AuthShield 360 — reference MySQL 8 schema (database.md)
--
-- Notes:
--  * The application uses logical references (no physical FOREIGN KEY constraints)
--    so that audit rows survive user deletion (compliance, BR-07).
--  * The `mysql` Spring profile runs with `spring.jpa.hibernate.ddl-auto=update`,
--    so this file is the canonical reference and can be applied by a DBA for a
--    controlled first provision. After the first boot you may switch ddl-auto to
--    `validate` to guarantee drift protection.
--  * All timestamps are UTC. Enums are stored as VARCHAR.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS authshield360
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE authshield360;

-- ---------------------------------------------------------------- users ----
CREATE TABLE IF NOT EXISTS users (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  username        VARCHAR(50)  NOT NULL,
  email           VARCHAR(255) NOT NULL,
  phone           VARCHAR(20)  NULL,
  password_hash   VARCHAR(100) NOT NULL,
  full_name       VARCHAR(120) NULL,
  role            VARCHAR(20)  NOT NULL,
  status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  mfa_enabled     BIT(1)       NOT NULL DEFAULT b'0',
  mfa_enrolled    BIT(1)       NOT NULL DEFAULT b'0',
  mfa_secret_enc  VARCHAR(255) NULL,
  failed_attempts INT          NOT NULL DEFAULT 0,
  lockout_level   INT          NOT NULL DEFAULT 0,
  locked_until    DATETIME(6)  NULL,
  created_at      DATETIME(6)  NOT NULL,
  updated_at      DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email),
  KEY ix_users_role (role)
) ENGINE=InnoDB;

-- ------------------------------------------------------- student_profiles ----
CREATE TABLE IF NOT EXISTS student_profiles (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  user_id          BIGINT       NOT NULL,
  student_code     VARCHAR(30)  NOT NULL,
  class_name       VARCHAR(60)  NULL,
  date_of_birth    DATE         NULL,
  guardian_contact VARCHAR(120) NULL,
  created_at       DATETIME(6)  NOT NULL,
  updated_at       DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_student_user (user_id),
  UNIQUE KEY uq_student_code (student_code)
) ENGINE=InnoDB;

-- ------------------------------------------------------- teacher_profiles ----
CREATE TABLE IF NOT EXISTS teacher_profiles (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  user_id       BIGINT      NOT NULL,
  employee_code VARCHAR(30) NOT NULL,
  department    VARCHAR(80) NULL,
  created_at    DATETIME(6) NOT NULL,
  updated_at    DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_teacher_user (user_id),
  UNIQUE KEY uq_teacher_code (employee_code)
) ENGINE=InnoDB;

-- ------------------------------------------------------------ classrooms ----
CREATE TABLE IF NOT EXISTS classrooms (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  code        VARCHAR(30)  NOT NULL,
  name        VARCHAR(120) NOT NULL,
  description VARCHAR(255) NULL,
  teacher_id  BIGINT       NULL,
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_classrooms_code (code),
  KEY ix_classrooms_teacher (teacher_id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------- enrollments ----
CREATE TABLE IF NOT EXISTS enrollments (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  classroom_id BIGINT      NOT NULL,
  student_id   BIGINT      NOT NULL,
  created_at   DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_enrollment (classroom_id, student_id),
  KEY ix_enrollments_student (student_id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------- assignments ----
CREATE TABLE IF NOT EXISTS assignments (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  title               VARCHAR(200) NOT NULL,
  description         VARCHAR(4000) NULL,
  classroom_id        BIGINT       NOT NULL,
  created_by          BIGINT       NOT NULL,
  due_at              DATETIME(6)  NOT NULL,
  allow_late          BIT(1)       NOT NULL DEFAULT b'1',
  late_cutoff_at      DATETIME(6)  NULL,
  late_penalty_pct    INT          NOT NULL DEFAULT 10,
  allow_resubmission  BIT(1)       NOT NULL DEFAULT b'1',
  max_attempts        INT          NOT NULL DEFAULT 3,
  max_score           INT          NOT NULL DEFAULT 100,
  status              VARCHAR(20)  NOT NULL,
  version             BIGINT       NULL,
  created_at          DATETIME(6)  NOT NULL,
  updated_at          DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY ix_assignments_classroom (classroom_id),
  KEY ix_assignments_status_due (status, due_at)
) ENGINE=InnoDB;

-- ------------------------------------------------ assignment_submissions ----
CREATE TABLE IF NOT EXISTS assignment_submissions (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  assignment_id     BIGINT       NOT NULL,
  student_id        BIGINT       NOT NULL,
  attempt_number    INT          NOT NULL,
  original_name     VARCHAR(255) NULL,
  stored_name       VARCHAR(255) NULL,
  content_type      VARCHAR(120) NULL,
  size_bytes        BIGINT       NULL,
  submitted_at      DATETIME(6)  NOT NULL,
  submission_status VARCHAR(20)  NOT NULL,
  score             INT          NULL,
  feedback          VARCHAR(2000) NULL,
  graded_by         BIGINT       NULL,
  graded_at         DATETIME(6)  NULL,
  version           BIGINT       NULL,
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_submission_attempt (assignment_id, student_id, attempt_number),
  KEY ix_submission_assignment (assignment_id),
  KEY ix_submission_student (student_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------- exam_results ----
CREATE TABLE IF NOT EXISTS exam_results (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  student_id  BIGINT       NOT NULL,
  subject     VARCHAR(80)  NOT NULL,
  exam_name   VARCHAR(120) NOT NULL,
  score       DOUBLE       NOT NULL,
  max_score   DOUBLE       NOT NULL DEFAULT 100,
  exam_date   DATE         NULL,
  recorded_by BIGINT       NULL,
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY ix_results_student (student_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------ audit_logs ----
CREATE TABLE IF NOT EXISTS audit_logs (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  event_time      DATETIME(6)  NOT NULL,
  event_id        VARCHAR(36)  NOT NULL,
  event_action    VARCHAR(40)  NOT NULL,
  status          VARCHAR(10)  NOT NULL,
  auth_factor     VARCHAR(20)  NULL,
  auth_mode       VARCHAR(4)   NULL,
  user_identifier VARCHAR(120) NULL,
  role            VARCHAR(20)  NULL,
  client_ip       VARCHAR(45)  NULL,
  user_agent      VARCHAR(512) NULL,
  requested_url   VARCHAR(512) NULL,
  session_id      VARCHAR(64)  NULL,
  failure_reason  VARCHAR(60)  NULL,
  correlation_id  VARCHAR(36)  NULL,
  detail          VARCHAR(2000) NULL,
  PRIMARY KEY (id),
  KEY ix_audit_event_time (event_time),
  KEY ix_audit_user (user_identifier),
  KEY ix_audit_action (event_action),
  KEY ix_audit_status (status),
  KEY ix_audit_ip (client_ip)
) ENGINE=InnoDB;

-- -------------------------------------------------------------- sessions ----
CREATE TABLE IF NOT EXISTS sessions (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  session_id    VARCHAR(64)  NOT NULL,
  user_id       BIGINT       NOT NULL,
  username      VARCHAR(50)  NOT NULL,
  role          VARCHAR(20)  NOT NULL,
  auth_method   VARCHAR(20)  NULL,
  issued_at     DATETIME(6)  NOT NULL,
  expires_at    DATETIME(6)  NOT NULL,
  last_seen_at  DATETIME(6)  NULL,
  client_ip     VARCHAR(45)  NULL,
  user_agent    VARCHAR(512) NULL,
  active        BIT(1)       NOT NULL DEFAULT b'1',
  revoked_at    DATETIME(6)  NULL,
  revoke_reason VARCHAR(60)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sessions_session_id (session_id),
  KEY ix_sessions_user_active (user_id, active),
  KEY ix_sessions_expires (expires_at)
) ENGINE=InnoDB;

-- ------------------------------------------------------------ otp_tokens ----
CREATE TABLE IF NOT EXISTS otp_tokens (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  user_identifier  VARCHAR(120) NOT NULL,
  factor           VARCHAR(20)  NOT NULL,
  code_hash        VARCHAR(128) NULL,
  totp_based       BIT(1)       NOT NULL DEFAULT b'0',
  delivery_channel VARCHAR(20)  NULL,
  issued_at        DATETIME(6)  NOT NULL,
  expires_at       DATETIME(6)  NOT NULL,
  consumed         BIT(1)       NOT NULL DEFAULT b'0',
  attempt_count    INT          NOT NULL DEFAULT 0,
  resend_count     INT          NOT NULL DEFAULT 0,
  last_sent_at     DATETIME(6)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_otp_user_factor (user_identifier, factor)
) ENGINE=InnoDB;

-- ----------------------------------------------------------- login_attempts --
CREATE TABLE IF NOT EXISTS login_attempts (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  user_identifier VARCHAR(120) NOT NULL,
  client_ip       VARCHAR(45)  NULL,
  occurred_at     DATETIME(6)  NOT NULL,
  success         BIT(1)       NOT NULL,
  reason          VARCHAR(60)  NULL,
  PRIMARY KEY (id),
  KEY ix_attempts_user_time (user_identifier, occurred_at)
) ENGINE=InnoDB;

-- ------------------------------------------------------------ auth_config ---
CREATE TABLE IF NOT EXISTS auth_config (
  id                       BIGINT      NOT NULL,
  mode                     VARCHAR(4)  NOT NULL,
  otp_type                 VARCHAR(20) NOT NULL,
  otp_length               INT         NOT NULL DEFAULT 6,
  otp_validity_seconds     INT         NOT NULL DEFAULT 90,
  resend_cooldown_seconds  INT         NOT NULL DEFAULT 60,
  max_resend               INT         NOT NULL DEFAULT 3,
  max_failed_attempts      INT         NOT NULL DEFAULT 5,
  lockout_durations_seconds VARCHAR(80) NOT NULL DEFAULT '60,300,900',
  require_captcha_after    INT         NOT NULL DEFAULT 3,
  smtp_host                VARCHAR(120) NULL,
  smtp_port                INT         NULL,
  smtp_username            VARCHAR(120) NULL,
  smtp_password_enc        VARCHAR(255) NULL,
  smtp_from                VARCHAR(160) NULL,
  email_otp_enabled        BIT(1)      NOT NULL DEFAULT b'1',
  updated_by               VARCHAR(50) NULL,
  updated_at               DATETIME(6) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

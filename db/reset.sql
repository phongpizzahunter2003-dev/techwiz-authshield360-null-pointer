-- ============================================================================
-- AuthShield 360 — reset script (UC-14)
-- Truncates all business data and restores the default configuration.
-- IMPORTANT: back up audit_logs first if evidence must be preserved.
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE assignment_submissions;
TRUNCATE TABLE assignments;
TRUNCATE TABLE exam_results;
TRUNCATE TABLE enrollments;
TRUNCATE TABLE classrooms;
TRUNCATE TABLE student_profiles;
TRUNCATE TABLE teacher_profiles;
TRUNCATE TABLE otp_tokens;
TRUNCATE TABLE login_attempts;
TRUNCATE TABLE sessions;
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE users;
TRUNCATE TABLE auth_config;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO auth_config (
  id, mode, otp_type, otp_length, otp_validity_seconds, resend_cooldown_seconds,
  max_resend, max_failed_attempts, lockout_durations_seconds, require_captcha_after,
  smtp_host, smtp_port, smtp_username, smtp_password_enc, smtp_from,
  email_otp_enabled, updated_by, updated_at
) VALUES (
  1, 'S1', 'TOTP', 6, 90, 60,
  3, 5, '60,300,900', 3,
  'sandbox.smtp.mailtrap.io', 2525, NULL, NULL, 'no-reply@authshield360.test',
  b'1', 'reset', UTC_TIMESTAMP(6)
);

-- The application re-seeds demo accounts on next start (users table is empty).

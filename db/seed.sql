-- ============================================================================
-- AuthShield 360 — seed data (simulated accounts only — BR-01)
--
-- User accounts are created by the application on first start (seed/DataSeeder.java)
-- because passwords must be hashed with BCrypt at runtime (BR-02) and MFA secrets
-- are AES-GCM encrypted with the deployment key (BR-10). Committing pre-computed
-- hashes/secrets to the repository would violate BR-10.
--
-- This file therefore seeds only the non-secret singleton configuration.
-- Start the backend once with the `mysql` profile and the demo accounts appear:
--   admin01 / admin123     (ADMIN)
--   teacher01 / teacher123 (TEACHER)
--   student01 / student123 (STUDENT)
--   student02 / student123 (STUDENT)
-- ============================================================================

INSERT INTO auth_config (
  id, mode, otp_type, otp_length, otp_validity_seconds, resend_cooldown_seconds,
  max_resend, max_failed_attempts, lockout_durations_seconds, require_captcha_after,
  smtp_host, smtp_port, smtp_username, smtp_password_enc, smtp_from,
  email_otp_enabled, updated_by, updated_at
) VALUES (
  1, 'S1', 'TOTP', 6, 90, 60,
  3, 5, '60,300,900', 3,
  'sandbox.smtp.mailtrap.io', 2525, NULL, NULL, 'no-reply@authshield360.test',
  b'1', 'seed', UTC_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE id = id;

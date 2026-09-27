package com.authshield360.auth.dto;

import com.authshield360.auth.AuthMode;

import java.time.Instant;

/** Safe view of the auth configuration (never exposes the SMTP password) — BR-10. */
public record AuthConfigResponse(
        AuthMode mode,
        String otpType,
        int otpLength,
        int otpValiditySeconds,
        int resendCooldownSeconds,
        int maxResend,
        int maxFailedAttempts,
        String lockoutDurationsSeconds,
        int requireCaptchaAfter,
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        boolean smtpPasswordSet,
        String smtpFrom,
        boolean emailOtpEnabled,
        String updatedBy,
        Instant updatedAt
) {
}

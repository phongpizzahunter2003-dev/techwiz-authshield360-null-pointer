package com.authshield360.auth.dto;

import com.authshield360.auth.AuthMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Partial update of the authentication configuration (UC-08). Null fields stay unchanged. */
public record UpdateConfigRequest(
        AuthMode mode,

        String otpType,

        @Min(value = 4, message = "OTP length must be at least 4.")
        @Max(value = 8, message = "OTP length must be at most 8.")
        Integer otpLength,

        @Min(value = 30, message = "OTP validity must be at least 30 seconds.")
        @Max(value = 300, message = "OTP validity must be at most 300 seconds.")
        Integer otpValiditySeconds,

        @Min(value = 10, message = "Resend cooldown must be at least 10 seconds.")
        @Max(value = 600, message = "Resend cooldown must be at most 600 seconds.")
        Integer resendCooldownSeconds,

        @Min(value = 0, message = "Maximum resends must be at least 0.")
        @Max(value = 10, message = "Maximum resends must be at most 10.")
        Integer maxResend,

        @Min(value = 1, message = "The failed-attempt threshold must be at least 1.")
        @Max(value = 50, message = "The failed-attempt threshold must be at most 50.")
        Integer maxFailedAttempts,

        String lockoutDurationsSeconds,

        @Min(value = 0)
        @Max(value = 20)
        Integer requireCaptchaAfter,

        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        String smtpPassword,
        String smtpFrom,
        Boolean emailOtpEnabled
) {
}

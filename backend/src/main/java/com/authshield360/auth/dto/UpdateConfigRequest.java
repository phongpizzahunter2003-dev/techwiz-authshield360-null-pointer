package com.authshield360.auth.dto;

import com.authshield360.auth.AuthMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Partial update of the auth configuration (UC-08). Null fields are left unchanged. */
public record UpdateConfigRequest(
        AuthMode mode,

        String otpType,

        @Min(value = 4, message = "Độ dài OTP tối thiểu 4.")
        @Max(value = 8, message = "Độ dài OTP tối đa 8.")
        Integer otpLength,

        @Min(value = 30, message = "Thời gian hiệu lực OTP tối thiểu 30 giây.")
        @Max(value = 300, message = "Thời gian hiệu lực OTP tối đa 300 giây.")
        Integer otpValiditySeconds,

        @Min(value = 10, message = "Thời gian chờ gửi lại tối thiểu 10 giây.")
        @Max(value = 600, message = "Thời gian chờ gửi lại tối đa 600 giây.")
        Integer resendCooldownSeconds,

        @Min(value = 0, message = "Số lần gửi lại tối thiểu 0.")
        @Max(value = 10, message = "Số lần gửi lại tối đa 10.")
        Integer maxResend,

        @Min(value = 1, message = "Ngưỡng đăng nhập sai tối thiểu 1.")
        @Max(value = 50, message = "Ngưỡng đăng nhập sai tối đa 50.")
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

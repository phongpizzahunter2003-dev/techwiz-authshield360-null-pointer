package com.authshield360.auth;

import jakarta.persistence.*;

import java.time.Instant;

/** Singleton runtime configuration (id = 1) — UC-08. Secrets are stored encrypted (BR-10). */
@Entity
@Table(name = "auth_config")
public class AuthConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Enumerated(EnumType.STRING)
    @Column(length = 4, nullable = false)
    private AuthMode mode = AuthMode.S2;

    @Column(name = "otp_type", length = 20, nullable = false)
    private String otpType = "TOTP";

    @Column(name = "otp_length", nullable = false)
    private int otpLength = 6;

    @Column(name = "otp_validity_seconds", nullable = false)
    private int otpValiditySeconds = 90;

    @Column(name = "resend_cooldown_seconds", nullable = false)
    private int resendCooldownSeconds = 60;

    @Column(name = "max_resend", nullable = false)
    private int maxResend = 3;

    @Column(name = "max_failed_attempts", nullable = false)
    private int maxFailedAttempts = 5;

    /** Comma-separated exponential lockout durations in seconds, e.g. "60,300,900". */
    @Column(name = "lockout_durations_seconds", length = 80, nullable = false)
    private String lockoutDurationsSeconds = "60,300,900";

    @Column(name = "require_captcha_after", nullable = false)
    private int requireCaptchaAfter = 3;

    @Column(name = "smtp_host", length = 120)
    private String smtpHost = "sandbox.smtp.mailtrap.io";

    @Column(name = "smtp_port")
    private Integer smtpPort = 2525;

    @Column(name = "smtp_username", length = 120)
    private String smtpUsername;

    @Column(name = "smtp_password_enc", length = 255)
    private String smtpPasswordEnc;

    @Column(name = "smtp_from", length = 160)
    private String smtpFrom = "no-reply@authshield360.test";

    @Column(name = "email_otp_enabled", nullable = false)
    private boolean emailOtpEnabled = true;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AuthMode getMode() { return mode; }
    public void setMode(AuthMode mode) { this.mode = mode; }
    public String getOtpType() { return otpType; }
    public void setOtpType(String otpType) { this.otpType = otpType; }
    public int getOtpLength() { return otpLength; }
    public void setOtpLength(int otpLength) { this.otpLength = otpLength; }
    public int getOtpValiditySeconds() { return otpValiditySeconds; }
    public void setOtpValiditySeconds(int otpValiditySeconds) { this.otpValiditySeconds = otpValiditySeconds; }
    public int getResendCooldownSeconds() { return resendCooldownSeconds; }
    public void setResendCooldownSeconds(int v) { this.resendCooldownSeconds = v; }
    public int getMaxResend() { return maxResend; }
    public void setMaxResend(int maxResend) { this.maxResend = maxResend; }
    public int getMaxFailedAttempts() { return maxFailedAttempts; }
    public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }
    public String getLockoutDurationsSeconds() { return lockoutDurationsSeconds; }
    public void setLockoutDurationsSeconds(String v) { this.lockoutDurationsSeconds = v; }
    public int getRequireCaptchaAfter() { return requireCaptchaAfter; }
    public void setRequireCaptchaAfter(int v) { this.requireCaptchaAfter = v; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }
    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }
    public String getSmtpPasswordEnc() { return smtpPasswordEnc; }
    public void setSmtpPasswordEnc(String smtpPasswordEnc) { this.smtpPasswordEnc = smtpPasswordEnc; }
    public String getSmtpFrom() { return smtpFrom; }
    public void setSmtpFrom(String smtpFrom) { this.smtpFrom = smtpFrom; }
    public boolean isEmailOtpEnabled() { return emailOtpEnabled; }
    public void setEmailOtpEnabled(boolean emailOtpEnabled) { this.emailOtpEnabled = emailOtpEnabled; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /** Parses the exponential lockout ladder. */
    public long[] lockoutLadder() {
        String[] parts = lockoutDurationsSeconds.split(",");
        long[] result = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Long.parseLong(parts[i].trim());
        }
        return result;
    }
}

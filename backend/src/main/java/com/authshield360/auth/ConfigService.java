package com.authshield360.auth;

import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditEvent;
import com.authshield360.audit.AuditService;
import com.authshield360.auth.dto.AuthConfigResponse;
import com.authshield360.auth.dto.UpdateConfigRequest;
import com.authshield360.security.CryptoService;
import com.authshield360.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Reads/updates the singleton authentication configuration (UC-08). */
@Service
public class ConfigService {

    private final AuthConfigRepository repository;
    private final CryptoService cryptoService;
    private final AuditService audit;

    public ConfigService(AuthConfigRepository repository, CryptoService cryptoService, AuditService audit) {
        this.repository = repository;
        this.cryptoService = cryptoService;
        this.audit = audit;
    }

    @Transactional
    public AuthConfig current() {
        return repository.findById(AuthConfig.SINGLETON_ID).orElseGet(() -> repository.save(new AuthConfig()));
    }

    @Transactional
    public AuthConfigResponse update(UpdateConfigRequest req) {
        AuthConfig config = current();
        List<String> changed = new ArrayList<>();

        if (req.mode() != null && req.mode() != config.getMode()) {
            changed.add("mode:" + config.getMode() + "->" + req.mode());
            config.setMode(req.mode());
        }
        if (req.otpType() != null && !req.otpType().isBlank() && !req.otpType().equals(config.getOtpType())) {
            changed.add("otpType:" + config.getOtpType() + "->" + req.otpType());
            config.setOtpType(req.otpType());
        }
        if (req.otpLength() != null && req.otpLength() != config.getOtpLength()) {
            changed.add("otpLength:" + config.getOtpLength() + "->" + req.otpLength());
            config.setOtpLength(req.otpLength());
        }
        if (req.otpValiditySeconds() != null && req.otpValiditySeconds() != config.getOtpValiditySeconds()) {
            changed.add("otpValiditySeconds:" + config.getOtpValiditySeconds() + "->" + req.otpValiditySeconds());
            config.setOtpValiditySeconds(req.otpValiditySeconds());
        }
        if (req.resendCooldownSeconds() != null && req.resendCooldownSeconds() != config.getResendCooldownSeconds()) {
            changed.add("resendCooldownSeconds:" + config.getResendCooldownSeconds() + "->" + req.resendCooldownSeconds());
            config.setResendCooldownSeconds(req.resendCooldownSeconds());
        }
        if (req.maxResend() != null && req.maxResend() != config.getMaxResend()) {
            changed.add("maxResend:" + config.getMaxResend() + "->" + req.maxResend());
            config.setMaxResend(req.maxResend());
        }
        if (req.maxFailedAttempts() != null && req.maxFailedAttempts() != config.getMaxFailedAttempts()) {
            changed.add("maxFailedAttempts:" + config.getMaxFailedAttempts() + "->" + req.maxFailedAttempts());
            config.setMaxFailedAttempts(req.maxFailedAttempts());
        }
        if (req.lockoutDurationsSeconds() != null && !req.lockoutDurationsSeconds().isBlank()
                && !req.lockoutDurationsSeconds().equals(config.getLockoutDurationsSeconds())) {
            config.setLockoutDurationsSeconds(req.lockoutDurationsSeconds());
            changed.add("lockoutDurationsSeconds:changed");
        }
        if (req.requireCaptchaAfter() != null && req.requireCaptchaAfter() != config.getRequireCaptchaAfter()) {
            changed.add("requireCaptchaAfter:" + config.getRequireCaptchaAfter() + "->" + req.requireCaptchaAfter());
            config.setRequireCaptchaAfter(req.requireCaptchaAfter());
        }
        if (req.smtpHost() != null && !req.smtpHost().equals(config.getSmtpHost())) {
            config.setSmtpHost(req.smtpHost());
            changed.add("smtpHost:changed");
        }
        if (req.smtpPort() != null && !req.smtpPort().equals(config.getSmtpPort())) {
            config.setSmtpPort(req.smtpPort());
            changed.add("smtpPort:changed");
        }
        if (req.smtpUsername() != null) {
            config.setSmtpUsername(req.smtpUsername());
            changed.add("smtpUsername:changed");
        }
        if (req.smtpPassword() != null && !req.smtpPassword().isBlank()) {
            config.setSmtpPasswordEnc(cryptoService.encrypt(req.smtpPassword()));
            changed.add("smtpPassword:changed");
        }
        if (req.smtpFrom() != null) {
            config.setSmtpFrom(req.smtpFrom());
            changed.add("smtpFrom:changed");
        }
        if (req.emailOtpEnabled() != null && !req.emailOtpEnabled().equals(config.isEmailOtpEnabled())) {
            config.setEmailOtpEnabled(req.emailOtpEnabled());
            changed.add("emailOtpEnabled:changed");
        }

        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(SecurityUtils.currentOrEmpty().map(cu -> cu.username()).orElse("system"));
        AuthConfig saved = repository.save(config);

        audit.record(AuditEvent.action(AuditAction.CONFIG_CHANGE).success()
                .user(config.getUpdatedBy())
                .detail("changedFields", String.join(",", changed))
                .detail("mode", saved.getMode().name()));

        return toResponse(saved);
    }

    public AuthConfigResponse toResponse(AuthConfig config) {
        return new AuthConfigResponse(
                config.getMode(),
                config.getOtpType(),
                config.getOtpLength(),
                config.getOtpValiditySeconds(),
                config.getResendCooldownSeconds(),
                config.getMaxResend(),
                config.getMaxFailedAttempts(),
                config.getLockoutDurationsSeconds(),
                config.getRequireCaptchaAfter(),
                config.getSmtpHost(),
                config.getSmtpPort(),
                config.getSmtpUsername(),
                config.getSmtpPasswordEnc() != null && !config.getSmtpPasswordEnc().isBlank(),
                config.getSmtpFrom(),
                config.isEmailOtpEnabled(),
                config.getUpdatedBy(),
                config.getUpdatedAt());
    }
}

package com.authshield360.audit;

import com.authshield360.common.Correlation;
import com.authshield360.common.WebUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Fluent description of an audit event. ip/ua/url/correlation are filled from the request context. */
public class AuditEvent {

    String action;
    AuditStatus status = AuditStatus.SUCCESS;
    String authFactor;
    String authMode;
    String userIdentifier;
    String role;
    String sessionId;
    String failureReason;
    final Map<String, Object> detail = new LinkedHashMap<>();

    public static AuditEvent action(String action) {
        AuditEvent e = new AuditEvent();
        e.action = action;
        return e;
    }

    public AuditEvent success() { this.status = AuditStatus.SUCCESS; return this; }
    public AuditEvent failure(String reason) { this.status = AuditStatus.FAILURE; this.failureReason = reason; return this; }
    public AuditEvent factor(String factor) { this.authFactor = factor; return this; }
    public AuditEvent mode(String mode) { this.authMode = mode; return this; }
    public AuditEvent user(String userIdentifier) { this.userIdentifier = userIdentifier; return this; }
    public AuditEvent role(String role) { this.role = role; return this; }
    public AuditEvent session(String sessionId) { this.sessionId = sessionId; return this; }
    public AuditEvent detail(String key, Object value) { this.detail.put(key, value); return this; }

    AuditLog toEntity(MaskingUtil masking) {
        AuditLog log = new AuditLog();
        log.setEventTime(java.time.Instant.now());
        log.setEventAction(action);
        log.setStatus(status.name());
        log.setAuthFactor(authFactor);
        log.setAuthMode(authMode);
        log.setUserIdentifier(userIdentifier);
        log.setRole(role);
        log.setSessionId(sessionId);
        log.setFailureReason(failureReason);
        log.setClientIp(WebUtils.clientIp());
        log.setUserAgent(WebUtils.userAgent());
        log.setRequestedUrl(WebUtils.requestedUrl());
        log.setCorrelationId(Correlation.current());
        log.setDetail(masking.toDetail(detail));
        return log;
    }
}

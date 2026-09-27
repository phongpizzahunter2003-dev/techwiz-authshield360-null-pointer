package com.authshield360.audit;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit record (BR-07). Audit rows are never updated or deleted by the app.
 * Note: user_identifier is stored as plain text (no FK) so logs survive user deletion.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "ix_audit_event_time", columnList = "event_time"),
        @Index(name = "ix_audit_user", columnList = "user_identifier"),
        @Index(name = "ix_audit_action", columnList = "event_action"),
        @Index(name = "ix_audit_status", columnList = "status"),
        @Index(name = "ix_audit_ip", columnList = "client_ip")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId = UUID.randomUUID().toString();

    @Column(name = "event_action", length = 40, nullable = false)
    private String eventAction;

    @Column(length = 10, nullable = false)
    private String status;

    @Column(name = "auth_factor", length = 20)
    private String authFactor;

    @Column(name = "auth_mode", length = 4)
    private String authMode;

    @Column(name = "user_identifier", length = 120)
    private String userIdentifier;

    @Column(length = 20)
    private String role;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "requested_url", length = 512)
    private String requestedUrl;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "failure_reason", length = 60)
    private String failureReason;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "detail", length = 2000)
    private String detail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventAction() { return eventAction; }
    public void setEventAction(String eventAction) { this.eventAction = eventAction; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAuthFactor() { return authFactor; }
    public void setAuthFactor(String authFactor) { this.authFactor = authFactor; }
    public String getAuthMode() { return authMode; }
    public void setAuthMode(String authMode) { this.authMode = authMode; }
    public String getUserIdentifier() { return userIdentifier; }
    public void setUserIdentifier(String userIdentifier) { this.userIdentifier = userIdentifier; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getRequestedUrl() { return requestedUrl; }
    public void setRequestedUrl(String requestedUrl) { this.requestedUrl = requestedUrl; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}

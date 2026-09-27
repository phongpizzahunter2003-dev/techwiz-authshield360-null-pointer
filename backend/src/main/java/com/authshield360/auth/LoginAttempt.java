package com.authshield360.auth;

import jakarta.persistence.*;

import java.time.Instant;

/** Raw failed/successful login attempt rows for evidence and windowed counting. */
@Entity
@Table(name = "login_attempts", indexes = @Index(name = "ix_attempts_user_time", columnList = "user_identifier,occurred_at"))
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_identifier", length = 120, nullable = false)
    private String userIdentifier;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 60)
    private String reason;

    protected LoginAttempt() { }

    public LoginAttempt(String userIdentifier, String clientIp, Instant occurredAt, boolean success, String reason) {
        this.userIdentifier = userIdentifier;
        this.clientIp = clientIp;
        this.occurredAt = occurredAt;
        this.success = success;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getUserIdentifier() { return userIdentifier; }
    public String getClientIp() { return clientIp; }
    public Instant getOccurredAt() { return occurredAt; }
    public boolean isSuccess() { return success; }
    public String getReason() { return reason; }
}

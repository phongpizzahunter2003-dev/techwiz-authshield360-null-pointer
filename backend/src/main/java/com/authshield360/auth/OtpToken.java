package com.authshield360.auth;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * The current OTP challenge for a (user, factor) pair. One row per pair; resending replaces the
 * code and invalidates the previous one (UC-04). Codes are stored hashed (BR-03/BR-10).
 */
@Entity
@Table(name = "otp_tokens", uniqueConstraints =
        @UniqueConstraint(name = "uq_otp_user_factor", columnNames = {"user_identifier", "factor"}))
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_identifier", length = 120, nullable = false)
    private String userIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private OtpFactor factor;

    @Column(name = "code_hash", length = 128)
    private String codeHash;

    @Column(name = "totp_based", nullable = false)
    private boolean totpBased = false;

    @Column(name = "delivery_channel", length = 20)
    private String deliveryChannel;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed = false;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "resend_count", nullable = false)
    private int resendCount = 0;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserIdentifier() { return userIdentifier; }
    public void setUserIdentifier(String userIdentifier) { this.userIdentifier = userIdentifier; }
    public OtpFactor getFactor() { return factor; }
    public void setFactor(OtpFactor factor) { this.factor = factor; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public boolean isTotpBased() { return totpBased; }
    public void setTotpBased(boolean totpBased) { this.totpBased = totpBased; }
    public String getDeliveryChannel() { return deliveryChannel; }
    public void setDeliveryChannel(String deliveryChannel) { this.deliveryChannel = deliveryChannel; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public int getResendCount() { return resendCount; }
    public void setResendCount(int resendCount) { this.resendCount = resendCount; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }
}

package com.authshield360.security;

import java.time.Instant;

/** Parsed token claims. */
public record TokenClaims(
        Long userId,
        String username,
        String role,
        String purpose,
        String sessionId,
        String jti,
        Instant issuedAt,
        Instant expiresAt
) {
    public static final String PURPOSE_ACCESS = "ACCESS";
    public static final String PURPOSE_MOBILE_OTP = "MOBILE_OTP";
    public static final String PURPOSE_EMAIL_OTP = "EMAIL_OTP";
}

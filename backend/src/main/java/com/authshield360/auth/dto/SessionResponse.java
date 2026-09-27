package com.authshield360.auth.dto;

import java.time.Instant;

/** Session info for the current user (UC-06). */
public record SessionResponse(
        Long userId,
        String username,
        String fullName,
        String email,
        String role,
        String sessionId,
        String authMethod,
        boolean mfaEnabled,
        boolean mfaEnrolled,
        String authModeOverride,
        String effectiveAuthMode,
        Instant issuedAt,
        Instant expiresAt
) {
}

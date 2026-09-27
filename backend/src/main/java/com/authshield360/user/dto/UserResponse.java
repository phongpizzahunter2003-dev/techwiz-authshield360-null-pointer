package com.authshield360.user.dto;

import com.authshield360.user.RoleType;
import com.authshield360.user.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String phone,
        String fullName,
        RoleType role,
        UserStatus status,
        boolean mfaEnabled,
        boolean mfaEnrolled,
        int failedAttempts,
        int lockoutLevel,
        Instant lockedUntil,
        Instant createdAt
) {
}

package com.authshield360.auth.dto;

import com.authshield360.user.RoleType;

public record UserSummary(
        Long id,
        String username,
        String fullName,
        String email,
        RoleType role,
        boolean mfaEnabled,
        boolean mfaEnrolled
) {
}

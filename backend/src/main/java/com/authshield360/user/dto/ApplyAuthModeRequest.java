package com.authshield360.user.dto;

import com.authshield360.user.RoleType;
import jakarta.validation.constraints.NotBlank;

/**
 * Bulk security-policy assignment (admin).
 *
 * @param mode S1 | S2 | S3 (S1 disables the MFA requirement, S2/S3 require it) or
 *             INHERIT to clear the per-user override and follow the global configuration.
 * @param role optional scope: null = every user, otherwise only that role.
 */
public record ApplyAuthModeRequest(
        @NotBlank(message = "Authentication mode is required.")
        String mode,
        RoleType role
) {
}

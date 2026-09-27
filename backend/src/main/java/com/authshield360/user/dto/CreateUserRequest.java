package com.authshield360.user.dto;

import com.authshield360.user.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Username is required.")
        @Size(max = 50, message = "Username must be at most 50 characters.")
        String username,

        @NotBlank(message = "Email is required.")
        @Email(message = "Email format is invalid.")
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^$|^[0-9+ .-]{6,20}$", message = "Phone number format is invalid.")
        String phone,

        @Size(max = 120, message = "Full name is too long.")
        String fullName,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
        String password,

        @NotNull(message = "Role is required.")
        RoleType role,

        /** Optional per-user mode: S1 | S2 | S3 (blank/null = follow the global configuration). */
        String authMode
) {
}

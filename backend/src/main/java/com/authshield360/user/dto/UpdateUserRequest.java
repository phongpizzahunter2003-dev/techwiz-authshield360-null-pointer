package com.authshield360.user.dto;

import com.authshield360.user.RoleType;
import com.authshield360.user.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email(message = "Email format is invalid.")
        @Size(max = 255)
        String email,

        @Size(max = 20, message = "Phone number is too long.")
        String phone,

        @Size(max = 120, message = "Full name is too long.")
        String fullName,

        RoleType role,

        UserStatus status,

        /** S1 | S2 | S3 | INHERIT (INHERIT clears the override). Null = unchanged. */
        String authMode,

        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
        String password
) {
}

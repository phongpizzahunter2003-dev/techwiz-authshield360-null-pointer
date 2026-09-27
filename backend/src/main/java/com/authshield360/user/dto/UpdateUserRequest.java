package com.authshield360.user.dto;

import com.authshield360.user.RoleType;
import com.authshield360.user.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email(message = "Email không đúng định dạng.")
        @Size(max = 255)
        String email,

        @Size(max = 20)
        String phone,

        @Size(max = 120)
        String fullName,

        RoleType role,

        UserStatus status,

        /** S1 | S2 | S3 | INHERIT (INHERIT clears the override). Null = unchanged. */
        String authMode,

        @Size(min = 8, max = 72, message = "Mật khẩu tối thiểu 8 ký tự.")
        String password
) {
}

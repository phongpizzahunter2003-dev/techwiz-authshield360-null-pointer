package com.authshield360.user.dto;

import com.authshield360.user.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống.")
        @Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự.")
        String username,

        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không đúng định dạng.")
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^$|^[0-9+ .-]{6,20}$", message = "Số điện thoại không đúng định dạng.")
        String phone,

        @Size(max = 120)
        String fullName,

        @NotBlank(message = "Mật khẩu không được để trống.")
        @Size(min = 8, max = 72, message = "Mật khẩu tối thiểu 8 ký tự.")
        String password,

        @NotNull(message = "Vai trò là bắt buộc.")
        RoleType role
) {
}

package com.authshield360.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống.")
        @Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự.")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống.")
        @Size(min = 8, max = 72, message = "Mật khẩu tối thiểu 8 ký tự.")
        String password,

        String captchaChallengeId,
        String captchaAnswer
) {
}

package com.authshield360.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank(message = "Phiên xác minh không hợp lệ.")
        String challengeToken,

        @NotBlank(message = "Mã OTP không được để trống.")
        @Pattern(regexp = "[0-9]{6}", message = "Mã OTP phải bao gồm 6 chữ số.")
        String code
) {
}

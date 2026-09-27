package com.authshield360.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank(message = "The verification session is invalid.")
        String challengeToken,

        @NotBlank(message = "The OTP code is required.")
        @Pattern(regexp = "[0-9]{6}", message = "The OTP must be exactly 6 digits.")
        String code
) {
}

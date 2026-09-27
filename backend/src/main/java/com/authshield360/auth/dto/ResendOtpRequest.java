package com.authshield360.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank(message = "Phiên xác minh không hợp lệ.")
        String challengeToken,
        String captchaChallengeId,
        String captchaAnswer
) {
}

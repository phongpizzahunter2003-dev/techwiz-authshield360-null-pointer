package com.authshield360.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank(message = "The verification session is invalid.")
        String challengeToken,
        String captchaChallengeId,
        String captchaAnswer
) {
}

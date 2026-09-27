package com.authshield360.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required.")
        @Size(max = 50, message = "Username must be at most 50 characters.")
        String username,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
        String password,

        String captchaChallengeId,
        String captchaAnswer
) {
}

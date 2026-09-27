package com.authshield360.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmMfaRequest(
        @NotBlank(message = "The confirmation code is required.")
        @Pattern(regexp = "[0-9]{6}", message = "The confirmation code must be exactly 6 digits.")
        String code
) {
}

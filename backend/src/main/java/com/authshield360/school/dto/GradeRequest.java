package com.authshield360.school.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GradeRequest(
        @NotNull(message = "Score is required.")
        @Min(value = 0, message = "Score is invalid.")
        Integer score,

        String feedback
) {
}

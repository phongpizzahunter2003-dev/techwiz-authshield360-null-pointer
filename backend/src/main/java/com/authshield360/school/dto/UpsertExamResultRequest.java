package com.authshield360.school.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpsertExamResultRequest(
        @NotNull(message = "Student is required.")
        Long studentId,

        @NotBlank(message = "Subject is required.")
        @Size(max = 80, message = "Subject must be at most 80 characters.")
        String subject,

        @NotBlank(message = "Exam name is required.")
        @Size(max = 120, message = "Exam name must be at most 120 characters.")
        String examName,

        @NotNull(message = "Score is required.")
        @DecimalMin(value = "0.0", message = "Score is invalid.")
        Double score,

        @DecimalMin(value = "1.0", message = "Maximum score is invalid.")
        Double maxScore,

        LocalDate examDate
) {
}

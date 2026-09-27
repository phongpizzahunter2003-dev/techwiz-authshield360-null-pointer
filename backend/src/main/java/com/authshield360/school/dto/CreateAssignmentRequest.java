package com.authshield360.school.dto;

import com.authshield360.school.AssignmentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateAssignmentRequest(
        @NotBlank(message = "Assignment title is required.")
        @Size(max = 200, message = "Title must be at most 200 characters.")
        String title,

        @Size(max = 4000, message = "Description is too long.")
        String description,

        @NotNull(message = "Class is required.")
        Long classroomId,

        @NotNull(message = "Due date is required.")
        Instant dueAt,

        Boolean allowLate,
        Instant lateCutoffAt,

        @Min(value = 0, message = "Late penalty is invalid.")
        Integer latePenaltyPct,

        Boolean allowResubmission,

        @Min(value = 1, message = "Maximum attempts must be at least 1.")
        Integer maxAttempts,

        @Min(value = 1, message = "Maximum score must be at least 1.")
        Integer maxScore,

        AssignmentStatus status
) {
}

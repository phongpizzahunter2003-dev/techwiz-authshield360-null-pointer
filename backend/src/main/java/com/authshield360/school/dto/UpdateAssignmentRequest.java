package com.authshield360.school.dto;

import com.authshield360.school.AssignmentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Partial update — null fields are left unchanged. */
public record UpdateAssignmentRequest(
        @Size(max = 200)
        String title,

        @Size(max = 4000)
        String description,

        Instant dueAt,
        Boolean allowLate,
        Instant lateCutoffAt,

        @Min(0)
        Integer latePenaltyPct,

        Boolean allowResubmission,

        @Min(1)
        Integer maxAttempts,

        @Min(1)
        Integer maxScore,

        AssignmentStatus status
) {
}

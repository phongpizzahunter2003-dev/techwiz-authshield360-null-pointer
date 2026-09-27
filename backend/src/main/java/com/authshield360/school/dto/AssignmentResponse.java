package com.authshield360.school.dto;

import com.authshield360.school.AssignmentStatus;

import java.time.Instant;
import java.util.List;

/**
 * Assignment view. For a student the trailing fields describe their own submission state,
 * including the UC-A3 lock reason and UC-A4 resubmission availability.
 */
public record AssignmentResponse(
        Long id,
        String title,
        String description,
        Long classroomId,
        String classroomName,
        Long createdBy,
        String createdByName,
        Instant dueAt,
        boolean allowLate,
        Instant lateCutoffAt,
        int latePenaltyPct,
        boolean allowResubmission,
        int maxAttempts,
        int maxScore,
        AssignmentStatus status,
        Instant createdAt,
        Integer yourAttempts,
        Boolean canSubmit,
        String lockReason,
        SubmissionResponse latestSubmission,
        List<SubmissionResponse> submissions
) {
}

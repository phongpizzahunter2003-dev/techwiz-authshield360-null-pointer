package com.authshield360.school.dto;

import com.authshield360.school.SubmissionStatus;

import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        int attemptNumber,
        String originalName,
        String contentType,
        Long sizeBytes,
        Instant submittedAt,
        SubmissionStatus submissionStatus,
        boolean late,
        Integer score,
        String feedback,
        Instant gradedAt,
        boolean current
) {
}

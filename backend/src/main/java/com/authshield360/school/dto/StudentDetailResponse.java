package com.authshield360.school.dto;

import java.util.List;

/**
 * Teacher-facing view of a single student, scoped to the classes the teacher owns:
 * profile, exam results and submissions for that teacher's assignments.
 */
public record StudentDetailResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        String studentCode,
        String className,
        List<String> classrooms,
        List<ExamResultResponse> results,
        List<SubmissionResponse> submissions
) {
}

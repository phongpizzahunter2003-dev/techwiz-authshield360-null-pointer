package com.authshield360.school.dto;

import java.time.LocalDate;

public record ExamResultResponse(
        Long id,
        Long studentId,
        String studentName,
        String subject,
        String examName,
        double score,
        double maxScore,
        LocalDate examDate
) {
}

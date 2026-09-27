package com.authshield360.school.dto;

import com.authshield360.school.AssignmentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateAssignmentRequest(
        @NotBlank(message = "Tiêu đề bài tập không được để trống.")
        @Size(max = 200)
        String title,

        @Size(max = 4000)
        String description,

        @NotNull(message = "Lớp học là bắt buộc.")
        Long classroomId,

        @NotNull(message = "Hạn nộp là bắt buộc.")
        Instant dueAt,

        Boolean allowLate,
        Instant lateCutoffAt,

        @Min(value = 0, message = "Mức trừ điểm không hợp lệ.")
        Integer latePenaltyPct,

        Boolean allowResubmission,

        @Min(value = 1, message = "Số lần nộp tối thiểu 1.")
        Integer maxAttempts,

        @Min(value = 1, message = "Điểm tối đa tối thiểu 1.")
        Integer maxScore,

        AssignmentStatus status
) {
}

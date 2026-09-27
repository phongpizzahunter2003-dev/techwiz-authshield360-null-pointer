package com.authshield360.school.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpsertExamResultRequest(
        @NotNull(message = "Học sinh là bắt buộc.")
        Long studentId,

        @NotBlank(message = "Môn học không được để trống.")
        @Size(max = 80)
        String subject,

        @NotBlank(message = "Tên bài thi không được để trống.")
        @Size(max = 120)
        String examName,

        @NotNull(message = "Điểm là bắt buộc.")
        @DecimalMin(value = "0.0", message = "Điểm không hợp lệ.")
        Double score,

        @DecimalMin(value = "1.0", message = "Điểm tối đa không hợp lệ.")
        Double maxScore,

        LocalDate examDate
) {
}

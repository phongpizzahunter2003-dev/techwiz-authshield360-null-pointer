package com.authshield360.school.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GradeRequest(
        @NotNull(message = "Điểm là bắt buộc.")
        @Min(value = 0, message = "Điểm không hợp lệ.")
        Integer score,

        String feedback
) {
}

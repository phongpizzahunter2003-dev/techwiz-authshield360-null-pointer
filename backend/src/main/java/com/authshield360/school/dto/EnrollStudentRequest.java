package com.authshield360.school.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollStudentRequest(@NotNull(message = "Học sinh là bắt buộc.") Long studentId) {
}

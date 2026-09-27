package com.authshield360.school.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollStudentRequest(@NotNull(message = "Student is required.") Long studentId) {
}

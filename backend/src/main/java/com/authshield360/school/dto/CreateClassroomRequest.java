package com.authshield360.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClassroomRequest(
        @NotBlank(message = "Class code is required.")
        @Size(max = 30, message = "Class code must be at most 30 characters.")
        String code,

        @NotBlank(message = "Class name is required.")
        @Size(max = 120, message = "Class name must be at most 120 characters.")
        String name,

        @Size(max = 255, message = "Description is too long.")
        String description,

        Long teacherId
) {
}

package com.authshield360.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClassroomRequest(
        @NotBlank(message = "Mã lớp không được để trống.")
        @Size(max = 30)
        String code,

        @NotBlank(message = "Tên lớp không được để trống.")
        @Size(max = 120)
        String name,

        @Size(max = 255)
        String description,

        Long teacherId
) {
}

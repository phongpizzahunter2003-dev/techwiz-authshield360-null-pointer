package com.authshield360.school.dto;

public record ClassroomResponse(
        Long id,
        String code,
        String name,
        String description,
        Long teacherId,
        String teacherName,
        long studentCount
) {
}

package com.authshield360.dashboard.dto;

import com.authshield360.school.dto.AssignmentResponse;
import com.authshield360.school.dto.ClassroomResponse;
import com.authshield360.school.dto.StatCard;

import java.util.List;

public record TeacherDashboardResponse(
        String greeting,
        String fullName,
        List<StatCard> stats,
        List<ClassroomResponse> classrooms,
        List<AssignmentResponse> assignments
) {
}

package com.authshield360.dashboard.dto;

import com.authshield360.school.dto.AssignmentResponse;
import com.authshield360.school.dto.StatCard;

import java.util.List;

public record StudentDashboardResponse(
        String greeting,
        String fullName,
        List<StatCard> stats,
        List<AssignmentResponse> assignments
) {
}

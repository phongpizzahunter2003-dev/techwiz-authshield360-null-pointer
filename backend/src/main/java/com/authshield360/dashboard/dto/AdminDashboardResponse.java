package com.authshield360.dashboard.dto;

import com.authshield360.audit.dto.AuditLogResponse;
import com.authshield360.school.dto.StatCard;

import java.util.List;

public record AdminDashboardResponse(
        String greeting,
        String fullName,
        List<StatCard> stats,
        List<AuditLogResponse> recentEvents
) {
}

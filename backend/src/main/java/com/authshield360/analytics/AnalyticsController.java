package com.authshield360.analytics;

import com.authshield360.analytics.dto.AnalyticsResponse;
import com.authshield360.common.ApiResponse;
import com.authshield360.school.AssignmentService;
import com.authshield360.school.dto.AssignmentResponse;
import com.authshield360.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/** Role-specific dashboard analytics with drill-down endpoints. */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AssignmentService assignmentService;

    public AnalyticsController(AnalyticsService analyticsService, AssignmentService assignmentService) {
        this.analyticsService = analyticsService;
        this.assignmentService = assignmentService;
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AnalyticsResponse> student() {
        return ApiResponse.ok(analyticsService.student(SecurityUtils.current()));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<AnalyticsResponse> teacher() {
        return ApiResponse.ok(analyticsService.teacher(SecurityUtils.current()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnalyticsResponse> admin() {
        return ApiResponse.ok(analyticsService.admin());
    }

    /**
     * Drill-down for the student "assignment status" chart.
     * bucket = ON_TIME | LATE | PENDING | LOCKED | ALL
     */
    @GetMapping("/student/assignments")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<AssignmentResponse>> studentAssignments(
            @RequestParam(defaultValue = "ALL") String bucket) {
        String normalized = bucket.toUpperCase(Locale.ROOT);
        List<AssignmentResponse> all = assignmentService.listFor(SecurityUtils.current());
        List<AssignmentResponse> filtered = all.stream().filter(a -> switch (normalized) {
            case "ON_TIME" -> a.latestSubmission() != null
                    && a.latestSubmission().submissionStatus() == com.authshield360.school.SubmissionStatus.ON_TIME;
            case "LATE" -> a.latestSubmission() != null
                    && a.latestSubmission().submissionStatus() == com.authshield360.school.SubmissionStatus.LATE;
            case "PENDING" -> a.latestSubmission() == null && Boolean.TRUE.equals(a.canSubmit());
            case "LOCKED" -> a.latestSubmission() == null && !Boolean.TRUE.equals(a.canSubmit());
            default -> true;
        }).toList();
        return ApiResponse.ok(filtered);
    }
}

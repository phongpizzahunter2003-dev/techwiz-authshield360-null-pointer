package com.authshield360.dashboard;

import com.authshield360.common.ApiResponse;
import com.authshield360.dashboard.dto.AdminDashboardResponse;
import com.authshield360.dashboard.dto.ComparisonResponse;
import com.authshield360.dashboard.dto.StudentDashboardResponse;
import com.authshield360.dashboard.dto.TeacherDashboardResponse;
import com.authshield360.security.CurrentUser;
import com.authshield360.security.SecurityUtils;
import com.authshield360.user.RoleType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** One endpoint per role dashboard plus the S1/S2/S3 comparison (UC-15). */
@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentDashboardResponse> student() {
        return ApiResponse.ok(dashboardService.student(SecurityUtils.current()));
    }

    @GetMapping("/dashboard/teacher")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<TeacherDashboardResponse> teacher() {
        return ApiResponse.ok(dashboardService.teacher(SecurityUtils.current()));
    }

    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDashboardResponse> admin() {
        return ApiResponse.ok(dashboardService.admin(SecurityUtils.current()));
    }

    @GetMapping("/admin/comparison")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ComparisonResponse> comparison() {
        return ApiResponse.ok(dashboardService.comparison());
    }

    /** Convenience dispatcher so the SPA can fetch "my dashboard" without knowing the role. */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<?> dashboard() {
        CurrentUser user = SecurityUtils.current();
        return switch (RoleType.valueOf(user.role())) {
            case STUDENT -> ApiResponse.ok(dashboardService.student(user));
            case TEACHER -> ApiResponse.ok(dashboardService.teacher(user));
            case ADMIN -> ApiResponse.ok(dashboardService.admin(user));
        };
    }
}

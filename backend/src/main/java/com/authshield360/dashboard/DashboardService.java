package com.authshield360.dashboard;

import com.authshield360.audit.AuditFilter;
import com.authshield360.audit.AuditLogRepository;
import com.authshield360.audit.AuditQueryService;
import com.authshield360.audit.AuditAction;
import com.authshield360.auth.AuthMode;
import com.authshield360.dashboard.dto.*;
import com.authshield360.school.AssignmentRepository;
import com.authshield360.school.AssignmentSubmissionRepository;
import com.authshield360.school.Classroom;
import com.authshield360.school.ClassroomRepository;
import com.authshield360.school.EnrollmentRepository;
import com.authshield360.school.SubmissionService;
import com.authshield360.school.AssignmentService;
import com.authshield360.school.SchoolMapper;
import com.authshield360.school.dto.AssignmentResponse;
import com.authshield360.school.dto.StatCard;
import com.authshield360.security.CurrentUser;
import com.authshield360.user.RoleType;
import com.authshield360.user.User;
import com.authshield360.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Aggregations behind the three role dashboards and the S1/S2/S3 comparison (UC-15). */
@Service
public class DashboardService {

    private final UserRepository users;
    private final ClassroomRepository classrooms;
    private final EnrollmentRepository enrollments;
    private final AssignmentRepository assignments;
    private final AssignmentSubmissionRepository submissions;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final SchoolMapper mapper;
    private final AuditQueryService auditQueryService;
    private final AuditLogRepository auditLogs;

    public DashboardService(UserRepository users, ClassroomRepository classrooms, EnrollmentRepository enrollments,
                            AssignmentRepository assignments, AssignmentSubmissionRepository submissions,
                            AssignmentService assignmentService, SubmissionService submissionService,
                            SchoolMapper mapper, AuditQueryService auditQueryService, AuditLogRepository auditLogs) {
        this.users = users;
        this.classrooms = classrooms;
        this.enrollments = enrollments;
        this.assignments = assignments;
        this.submissions = submissions;
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.mapper = mapper;
        this.auditQueryService = auditQueryService;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public StudentDashboardResponse student(CurrentUser viewer) {
        User user = users.findById(viewer.userId()).orElseThrow();
        List<AssignmentResponse> items = assignmentService.listFor(viewer);
        long submitted = items.stream().filter(a -> a.latestSubmission() != null).count();
        long late = items.stream().filter(a -> a.latestSubmission() != null && a.latestSubmission().late()).count();
        long pending = items.stream().filter(a -> Boolean.TRUE.equals(a.canSubmit())).count();

        List<StatCard> stats = List.of(
                new StatCard("classes", "Lớp đang học", String.valueOf(enrollments.findByStudentId(viewer.userId()).size()), "brand"),
                new StatCard("assignments", "Tổng bài tập", String.valueOf(items.size()), "sky"),
                new StatCard("submitted", "Đã nộp", String.valueOf(submitted), "accent"),
                new StatCard("pending", "Cần nộp", String.valueOf(pending), "sun"),
                new StatCard("late", "Nộp muộn", String.valueOf(late), "coral"));

        return new StudentDashboardResponse(greeting(user), user.getFullName(), stats, items);
    }

    @Transactional(readOnly = true)
    public TeacherDashboardResponse teacher(CurrentUser viewer) {
        User user = users.findById(viewer.userId()).orElseThrow();
        List<Classroom> owned = classrooms.findByTeacherIdOrderByNameAsc(viewer.userId());
        List<com.authshield360.school.dto.ClassroomResponse> classroomResponses =
                owned.stream().map(mapper::toClassroom).toList();
        List<AssignmentResponse> items = assignmentService.listFor(viewer);

        long studentCount = owned.stream().mapToLong(c -> enrollments.countByClassroomId(c.getId())).sum();
        long toGrade = items.stream()
                .flatMap(a -> submissions.findByAssignmentIdOrderBySubmittedAtDesc(a.id()).stream())
                .filter(s -> s.getScore() == null)
                .count();

        List<StatCard> stats = List.of(
                new StatCard("classes", "Lớp phụ trách", String.valueOf(owned.size()), "brand"),
                new StatCard("students", "Học sinh", String.valueOf(studentCount), "sky"),
                new StatCard("assignments", "Bài tập", String.valueOf(items.size()), "accent"),
                new StatCard("toGrade", "Chờ chấm", String.valueOf(toGrade), "sun"));

        return new TeacherDashboardResponse(greeting(user), user.getFullName(), stats, classroomResponses, items);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse admin(CurrentUser viewer) {
        User user = users.findById(viewer.userId()).orElseThrow();

        long totalUsers = users.count();
        long students = users.countByRole(RoleType.STUDENT);
        long teachers = users.countByRole(RoleType.TEACHER);
        long lockouts = auditLogs.countByEventAction(AuditAction.LOCKOUT_TRIGGERED);
        long loginFailures = auditLogs.countByEventAction(AuditAction.LOGIN_FAIL);
        long privilegeViolations = auditLogs.countByEventAction(AuditAction.PRIVILEGE_VIOLATION);

        List<StatCard> stats = List.of(
                new StatCard("users", "Tổng người dùng", String.valueOf(totalUsers), "brand"),
                new StatCard("students", "Học sinh", String.valueOf(students), "sky"),
                new StatCard("teachers", "Giáo viên", String.valueOf(teachers), "accent"),
                new StatCard("assignments", "Bài tập", String.valueOf(assignments.count()), "sun"),
                new StatCard("submissions", "Lượt nộp bài", String.valueOf(submissions.count()), "brand"),
                new StatCard("lockouts", "Lần khóa tài khoản", String.valueOf(lockouts), "coral"),
                new StatCard("loginFailures", "Đăng nhập thất bại", String.valueOf(loginFailures), "coral"),
                new StatCard("privilegeViolations", "Vi phạm phân quyền", String.valueOf(privilegeViolations), "coral"));

        var recent = auditQueryService
                .list(AuditFilter.empty(), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "eventTime")))
                .stream().map(auditQueryService::toResponse).toList();

        return new AdminDashboardResponse(greeting(user), user.getFullName(), stats, recent);
    }

    @Transactional(readOnly = true)
    public ComparisonResponse comparison() {
        Map<String, long[]> byMode = new HashMap<>(); // [successes, failures]
        for (Object[] row : auditLogs.loginCountsByMode()) {
            String mode = (String) row[0];
            String status = (String) row[1];
            long count = ((Number) row[2]).longValue();
            long[] cell = byMode.computeIfAbsent(mode, k -> new long[2]);
            if ("SUCCESS".equals(status)) {
                cell[0] += count;
            } else {
                cell[1] += count;
            }
        }
        Map<String, Long> otpFailures = new HashMap<>();
        for (Object[] row : auditLogs.otpFailuresByMode()) {
            otpFailures.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<ModeComparison> modes = new ArrayList<>();
        modes.add(buildComparison(AuthMode.S1, "Chỉ mật khẩu",
                "Thấp – trung bình", "Rất dễ", "Không có lớp bảo vệ nếu lộ mật khẩu", byMode, otpFailures));
        modes.add(buildComparison(AuthMode.S2, "Mật khẩu + OTP",
                "Trung bình – cao", "Dễ – trung bình", "Lộ mật khẩu đơn lẻ là không đủ", byMode, otpFailures));
        modes.add(buildComparison(AuthMode.S3, "Mật khẩu + Mobile OTP + Email OTP",
                "Cao hơn", "Trung bình; phụ thuộc truy cập email",
                "Thêm rào chắn nếu email chưa bị chiếm", byMode, otpFailures));

        return new ComparisonResponse(modes, Instant.now(),
                "Số liệu tổng hợp từ audit_logs theo chế độ xác thực. BR-09 yêu cầu tối thiểu 3 lần chạy mỗi chế độ.");
    }

    private ModeComparison buildComparison(AuthMode mode, String name, String security, String usability,
                                           String risk, Map<String, long[]> byMode, Map<String, Long> otpFailures) {
        long[] cell = byMode.getOrDefault(mode.name(), new long[]{0, 0});
        return new ModeComparison(mode, name, security, usability, risk, cell[0], cell[1],
                otpFailures.getOrDefault(mode.name(), 0L));
    }

    private String greeting(User user) {
        String name = (user.getFullName() == null || user.getFullName().isBlank())
                ? user.getUsername() : user.getFullName();
        return "Xin chào, " + name + "!";
    }
}

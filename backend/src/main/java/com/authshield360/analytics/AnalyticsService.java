package com.authshield360.analytics;

import com.authshield360.analytics.dto.AnalyticsResponse;
import com.authshield360.analytics.dto.ChartSeries;
import com.authshield360.analytics.dto.ChartSlice;
import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditLogRepository;
import com.authshield360.auth.ConfigService;
import com.authshield360.school.AssignmentRepository;
import com.authshield360.school.AssignmentSubmission;
import com.authshield360.school.AssignmentSubmissionRepository;
import com.authshield360.school.Classroom;
import com.authshield360.school.ClassroomRepository;
import com.authshield360.school.EnrollmentRepository;
import com.authshield360.school.ExamResultRepository;
import com.authshield360.school.SubmissionStatus;
import com.authshield360.school.AssignmentService;
import com.authshield360.school.dto.AssignmentResponse;
import com.authshield360.security.CurrentUser;
import com.authshield360.user.RoleType;
import com.authshield360.user.User;
import com.authshield360.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregations behind the clickable role dashboards.
 * Each chart slice carries the drill-down route the UI should open.
 */
@Service
public class AnalyticsService {

    private static final int DAY_WINDOW = 7;

    private final UserRepository users;
    private final ClassroomRepository classrooms;
    private final EnrollmentRepository enrollments;
    private final AssignmentRepository assignments;
    private final AssignmentSubmissionRepository submissions;
    private final ExamResultRepository examResults;
    private final AuditLogRepository auditLogs;
    private final AssignmentService assignmentService;
    private final ConfigService configService;

    public AnalyticsService(UserRepository users, ClassroomRepository classrooms, EnrollmentRepository enrollments,
                            AssignmentRepository assignments, AssignmentSubmissionRepository submissions,
                            ExamResultRepository examResults, AuditLogRepository auditLogs,
                            AssignmentService assignmentService, ConfigService configService) {
        this.users = users;
        this.classrooms = classrooms;
        this.enrollments = enrollments;
        this.assignments = assignments;
        this.submissions = submissions;
        this.examResults = examResults;
        this.auditLogs = auditLogs;
        this.assignmentService = assignmentService;
        this.configService = configService;
    }

    // ---------------------------------------------------------------- STUDENT

    @Transactional(readOnly = true)
    public AnalyticsResponse student(CurrentUser viewer) {
        User user = users.findById(viewer.userId()).orElseThrow();
        List<AssignmentResponse> items = assignmentService.listFor(viewer);

        long onTime = 0;
        long late = 0;
        long pending = 0;
        long locked = 0;
        List<ChartSlice> attempts = new ArrayList<>();
        for (AssignmentResponse a : items) {
            if (a.latestSubmission() != null) {
                if (a.latestSubmission().submissionStatus() == SubmissionStatus.LATE) {
                    late++;
                } else {
                    onTime++;
                }
            } else if (Boolean.TRUE.equals(a.canSubmit())) {
                pending++;
            } else {
                locked++;
            }
            attempts.add(new ChartSlice("a" + a.id(), truncate(a.title()), a.yourAttempts() == null ? 0 : a.yourAttempts(),
                    "sky", "/student/assignments/" + a.id()));
        }

        ChartSeries statusChart = new ChartSeries("assignmentStatus", "Trạng thái bài tập", ChartSeries.PIE,
                "bài tập", "Bấm vào một phần để xem danh sách bài tập tương ứng.",
                List.of(
                        new ChartSlice("ON_TIME", "Đúng hạn", onTime, "accent", "/student/assignments?bucket=ON_TIME"),
                        new ChartSlice("LATE", "Nộp muộn", late, "coral", "/student/assignments?bucket=LATE"),
                        new ChartSlice("PENDING", "Cần nộp", pending, "sun", "/student/assignments?bucket=PENDING"),
                        new ChartSlice("LOCKED", "Không thể nộp", locked, "brand", "/student/assignments?bucket=LOCKED")));

        List<ChartSlice> scoreSlices = new ArrayList<>();
        examResults.findByStudentIdOrderByExamDateDesc(user.getId()).forEach(r ->
                scoreSlices.add(new ChartSlice("r" + r.getId(),
                        truncate(r.getSubject() + " · " + r.getExamName()),
                        (long) Math.round(r.getScore()), "sky",
                        "/student/results")));

        List<ChartSeries> charts = new ArrayList<>();
        charts.add(statusChart);
        charts.add(new ChartSeries("attemptsByAssignment", "Số lần nộp theo bài tập", ChartSeries.BAR,
                "lần nộp", "Bấm vào cột để mở chi tiết bài tập.", attempts));
        charts.add(new ChartSeries("scores", "Điểm theo bài thi", ChartSeries.BAR,
                "điểm", "Bấm vào cột để xem toàn bộ kết quả thi.", scoreSlices));

        return new AnalyticsResponse("STUDENT", "Xin chào, " + displayName(user) + "!", charts);
    }

    // ---------------------------------------------------------------- TEACHER

    @Transactional(readOnly = true)
    public AnalyticsResponse teacher(CurrentUser viewer) {
        User user = users.findById(viewer.userId()).orElseThrow();
        List<AssignmentResponse> items = assignmentService.listFor(viewer);
        List<Classroom> owned = classrooms.findByTeacherIdOrderByNameAsc(user.getId());

        List<ChartSlice> byAssignment = new ArrayList<>();
        long graded = 0;
        long awaiting = 0;
        for (AssignmentResponse a : items) {
            List<AssignmentSubmission> subs = submissions.findByAssignmentIdOrderBySubmittedAtDesc(a.id());
            byAssignment.add(new ChartSlice("a" + a.id(), truncate(a.title()), subs.size(), "brand",
                    "/teacher/assignments/" + a.id()));
            graded += subs.stream().filter(s -> s.getScore() != null).count();
            awaiting += subs.stream().filter(s -> s.getScore() == null).count();
        }

        List<ChartSlice> perClass = new ArrayList<>();
        for (Classroom c : owned) {
            perClass.add(new ChartSlice("c" + c.getId(), truncate(c.getName()),
                    enrollments.countByClassroomId(c.getId()), "accent", "/teacher/classes/" + c.getId()));
        }

        List<ChartSeries> charts = new ArrayList<>();
        charts.add(new ChartSeries("submissionsByAssignment", "Bài nộp theo bài tập", ChartSeries.BAR,
                "bài nộp", "Bấm vào cột để mở bài tập và chấm điểm.", byAssignment));
        charts.add(new ChartSeries("gradingProgress", "Tiến độ chấm điểm", ChartSeries.PIE,
                "bài nộp", "Bấm để mở danh sách bài tập cần chấm.",
                List.of(
                        new ChartSlice("GRADED", "Đã chấm", graded, "accent", "/teacher/assignments"),
                        new ChartSlice("PENDING", "Chờ chấm", awaiting, "sun", "/teacher/assignments"))));
        charts.add(new ChartSeries("studentsPerClass", "Học sinh theo lớp", ChartSeries.BAR,
                "học sinh", "Bấm vào cột để xem chi tiết lớp.", perClass));

        return new AnalyticsResponse("TEACHER", "Xin chào, " + displayName(user) + "!", charts);
    }

    // ------------------------------------------------------------------ ADMIN

    @Transactional(readOnly = true)
    public AnalyticsResponse admin() {
        User user = currentAdmin();

        long[] modeTotals = new long[3];
        for (Object[] row : auditLogs.loginCountsByMode()) {
            String mode = (String) row[0];
            String status = (String) row[1];
            long count = ((Number) row[2]).longValue();
            if ("SUCCESS".equals(status)) {
                if ("S1".equals(mode)) modeTotals[0] += count;
                if ("S2".equals(mode)) modeTotals[1] += count;
                if ("S3".equals(mode)) modeTotals[2] += count;
            }
        }

        List<ChartSeries> charts = new ArrayList<>();
        charts.add(new ChartSeries("loginsByMode", "Đăng nhập thành công theo chế độ", ChartSeries.BAR,
                "lượt đăng nhập", "Bấm vào cột để lọc nhật ký theo chế độ " + configService.current().getMode(),
                List.of(
                        new ChartSlice("S1", "S1 · Chỉ mật khẩu", modeTotals[0], "coral", "/admin/audit-logs?mode=S1"),
                        new ChartSlice("S2", "S2 · + Mobile OTP", modeTotals[1], "sun", "/admin/audit-logs?mode=S2"),
                        new ChartSlice("S3", "S3 · + Email OTP", modeTotals[2], "accent", "/admin/audit-logs?mode=S3"))));

        charts.add(new ChartSeries("usersByRole", "Người dùng theo vai trò", ChartSeries.PIE,
                "tài khoản", "Bấm để mở danh sách người dùng theo vai trò.",
                List.of(
                        new ChartSlice("STUDENT", "Học sinh", users.countByRole(RoleType.STUDENT), "sky",
                                "/admin/users?role=STUDENT"),
                        new ChartSlice("TEACHER", "Giáo viên", users.countByRole(RoleType.TEACHER), "accent",
                                "/admin/users?role=TEACHER"),
                        new ChartSlice("ADMIN", "Quản trị viên", users.countByRole(RoleType.ADMIN), "brand",
                                "/admin/users?role=ADMIN"))));

        charts.add(new ChartSeries("securityEvents", "Sự kiện bảo mật", ChartSeries.BAR,
                "sự kiện", "Bấm vào cột để xem các bản ghi tương ứng trong nhật ký.",
                List.of(
                        new ChartSlice("LOGIN_FAIL", "Đăng nhập thất bại",
                                auditLogs.countByEventAction(AuditAction.LOGIN_FAIL), "coral",
                                "/admin/audit-logs?action=LOGIN_FAIL"),
                        new ChartSlice("LOCKOUT_TRIGGERED", "Khóa tài khoản",
                                auditLogs.countByEventAction(AuditAction.LOCKOUT_TRIGGERED), "sun",
                                "/admin/audit-logs?action=LOCKOUT_TRIGGERED"),
                        new ChartSlice("OTP_VERIFY_FAIL", "OTP sai",
                                auditLogs.countByEventAction(AuditAction.OTP_VERIFY_FAIL), "sun",
                                "/admin/audit-logs?action=OTP_VERIFY_FAIL"),
                        new ChartSlice("PRIVILEGE_VIOLATION", "Vi phạm phân quyền",
                                auditLogs.countByEventAction(AuditAction.PRIVILEGE_VIOLATION), "coral",
                                "/admin/audit-logs?action=PRIVILEGE_VIOLATION"),
                        new ChartSlice("SESSION_REPLAY_ATTEMPT", "Tái sử dụng phiên",
                                auditLogs.countByEventAction(AuditAction.SESSION_REPLAY_ATTEMPT), "coral",
                                "/admin/audit-logs?action=SESSION_REPLAY_ATTEMPT"))));

        charts.add(new ChartSeries("eventsByDay", "Sự kiện " + DAY_WINDOW + " ngày gần nhất", ChartSeries.LINE,
                "sự kiện/ngày", "Bấm vào điểm để mở nhật ký trong ngày.",
                eventsPerDay()));

        return new AnalyticsResponse("ADMIN", "Xin chào, " + displayName(user) + "!", charts);
    }

    private List<ChartSlice> eventsPerDay() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant from = today.minusDays(DAY_WINDOW - 1L).atStartOfDay(ZoneOffset.UTC).toInstant();
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (long i = DAY_WINDOW - 1; i >= 0; i--) {
            counts.put(today.minusDays(i), 0L);
        }
        for (Instant t : auditLogs.findEventTimesAfter(from)) {
            LocalDate day = t.atZone(ZoneOffset.UTC).toLocalDate();
            counts.computeIfPresent(day, (k, v) -> v + 1);
        }
        List<ChartSlice> slices = new ArrayList<>();
        counts.forEach((day, count) -> slices.add(new ChartSlice(day.toString(),
                day.getDayOfMonth() + "/" + day.getMonthValue(), count, "brand",
                "/admin/audit-logs?from=" + day.atStartOfDay(ZoneOffset.UTC).toInstant()
                        + "&to=" + day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())));
        return slices;
    }

    private User currentAdmin() {
        Long id = com.authshield360.security.SecurityUtils.current().userId();
        return users.findById(id).orElseThrow();
    }

    private String displayName(User user) {
        return (user.getFullName() == null || user.getFullName().isBlank()) ? user.getUsername() : user.getFullName();
    }

    private String truncate(String value) {
        if (value == null) return "";
        return value.length() > 26 ? value.substring(0, 24) + "…" : value;
    }
}

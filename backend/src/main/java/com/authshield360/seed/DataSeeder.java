package com.authshield360.seed;

import com.authshield360.auth.AuthConfig;
import com.authshield360.auth.AuthConfigRepository;
import com.authshield360.auth.AuthMode;
import com.authshield360.school.*;
import com.authshield360.user.RoleType;
import com.authshield360.user.UserRepository;
import com.authshield360.user.UserService;
import com.authshield360.user.dto.CreateUserRequest;
import com.authshield360.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Seeds simulated lab data only (BR-01): fake emails/phones, no real people.
 * Idempotent: runs only when there are no users yet.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserService userService;
    private final UserRepository users;
    private final ClassroomRepository classrooms;
    private final EnrollmentRepository enrollments;
    private final AssignmentRepository assignments;
    private final ExamResultRepository examResults;
    private final AuthConfigRepository authConfig;

    public DataSeeder(UserService userService, UserRepository users, ClassroomRepository classrooms,
                      EnrollmentRepository enrollments, AssignmentRepository assignments,
                      ExamResultRepository examResults, AuthConfigRepository authConfig) {
        this.userService = userService;
        this.users = users;
        this.classrooms = classrooms;
        this.enrollments = enrollments;
        this.assignments = assignments;
        this.examResults = examResults;
        this.authConfig = authConfig;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (authConfig.findById(AuthConfig.SINGLETON_ID).isEmpty()) {
            AuthConfig config = new AuthConfig();
            config.setMode(AuthMode.S1); // baseline; admin can switch to S2/S3 in the UI (UC-08)
            config.setOtpType("TOTP");
            config.setOtpValiditySeconds(90);
            authConfig.save(config);
        }

        if (users.count() > 0) {
            return;
        }
        log.info("Seeding AuthShield 360 lab data (simulated accounts only — BR-01)");

        userService.create(new CreateUserRequest(
                "admin01", "admin01@mailtrap.io", "0912000001", "Quản trị viên", "admin123", RoleType.ADMIN, null));
        UserResponse teacher = userService.create(new CreateUserRequest(
                "teacher01", "teacher01@mailtrap.io", "0912000002", "Nguyễn Văn Tùng", "teacher123", RoleType.TEACHER, null));
        UserResponse student1 = userService.create(new CreateUserRequest(
                "student01", "student01@mailtrap.io", "0912000003", "Trần Minh Anh", "student123", RoleType.STUDENT, null));
        UserResponse student2 = userService.create(new CreateUserRequest(
                "student02", "student02@mailtrap.io", "0912000004", "Lê Gia Bảo", "student123", RoleType.STUDENT, null));

        Classroom cs101 = new Classroom();
        cs101.setCode("CS101");
        cs101.setName("Lập trình cơ bản - CS101");
        cs101.setDescription("Lớp mẫu cho kiểm thử AuthShield 360");
        cs101.setTeacherId(teacher.id());
        cs101 = classrooms.save(cs101);

        enrollments.save(new Enrollment(cs101.getId(), student1.id()));
        enrollments.save(new Enrollment(cs101.getId(), student2.id()));

        Instant now = Instant.now();

        Assignment onTime = new Assignment();
        onTime.setTitle("Bài tập 1 - Vòng lặp và mảng");
        onTime.setDescription("Nộp bài trước hạn để đạt điểm tối đa.");
        onTime.setClassroomId(cs101.getId());
        onTime.setCreatedBy(teacher.id());
        onTime.setDueAt(now.plus(7, ChronoUnit.DAYS));
        onTime.setAllowLate(true);
        onTime.setLateCutoffAt(now.plus(10, ChronoUnit.DAYS));
        onTime.setAllowResubmission(true);
        onTime.setMaxAttempts(3);
        onTime.setStatus(AssignmentStatus.PUBLISHED);
        assignments.save(onTime);

        Assignment late = new Assignment();
        late.setTitle("Bài tập 2 - Đệ quy (cho phép nộp muộn)");
        late.setDescription("Đã quá hạn nhưng vẫn cho phép nộp muộn, bị trừ điểm.");
        late.setClassroomId(cs101.getId());
        late.setCreatedBy(teacher.id());
        late.setDueAt(now.minus(2, ChronoUnit.DAYS));
        late.setAllowLate(true);
        late.setLateCutoffAt(now.plus(5, ChronoUnit.DAYS));
        late.setLatePenaltyPct(10);
        late.setAllowResubmission(true);
        late.setMaxAttempts(3);
        late.setStatus(AssignmentStatus.PUBLISHED);
        assignments.save(late);

        Assignment noLate = new Assignment();
        noLate.setTitle("Bài tập 3 - Cấu trúc dữ liệu (không nhận muộn)");
        noLate.setDescription("Đã quá hạn và không cho phép nộp muộn.");
        noLate.setClassroomId(cs101.getId());
        noLate.setCreatedBy(teacher.id());
        noLate.setDueAt(now.minus(2, ChronoUnit.DAYS));
        noLate.setAllowLate(false);
        noLate.setAllowResubmission(true);
        noLate.setMaxAttempts(3);
        noLate.setStatus(AssignmentStatus.PUBLISHED);
        assignments.save(noLate);

        Assignment closed = new Assignment();
        closed.setTitle("Kiểm tra giữa kỳ (đã đóng)");
        closed.setDescription("Bài kiểm tra đã đóng, học sinh không thể nộp hay cập nhật.");
        closed.setClassroomId(cs101.getId());
        closed.setCreatedBy(teacher.id());
        closed.setDueAt(now.minus(1, ChronoUnit.DAYS));
        closed.setAllowLate(false);
        closed.setAllowResubmission(false);
        closed.setMaxAttempts(1);
        closed.setStatus(AssignmentStatus.CLOSED);
        assignments.save(closed);

        Assignment resubmit = new Assignment();
        resubmit.setTitle("Bài tập 4 - Cho phép cập nhật bài nộp");
        resubmit.setDescription("Có thể nộp lại và cập nhật tệp nhiều lần trước hạn.");
        resubmit.setClassroomId(cs101.getId());
        resubmit.setCreatedBy(teacher.id());
        resubmit.setDueAt(now.plus(3, ChronoUnit.DAYS));
        resubmit.setAllowLate(true);
        resubmit.setLateCutoffAt(now.plus(6, ChronoUnit.DAYS));
        resubmit.setAllowResubmission(true);
        resubmit.setMaxAttempts(5);
        resubmit.setStatus(AssignmentStatus.PUBLISHED);
        assignments.save(resubmit);

        ExamResult result = new ExamResult();
        result.setStudentId(student1.id());
        result.setSubject("Lập trình");
        result.setExamName("Kiểm tra 15 phút");
        result.setScore(8.5);
        result.setMaxScore(10);
        result.setExamDate(LocalDate.now().minusDays(5));
        result.setRecordedBy(teacher.id());
        examResults.save(result);

        log.info("Seed complete. Accounts: admin01/admin123, teacher01/teacher123, student01|student02/student123");
    }
}

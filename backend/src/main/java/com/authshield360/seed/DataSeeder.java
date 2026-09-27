package com.authshield360.seed;

import com.authshield360.auth.AuthConfig;
import com.authshield360.auth.AuthConfigRepository;
import com.authshield360.auth.AuthMode;
import com.authshield360.notification.NotificationService;
import com.authshield360.notification.NotificationType;
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
import java.util.List;

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
    private final NotificationService notifications;

    public DataSeeder(UserService userService, UserRepository users, ClassroomRepository classrooms,
                      EnrollmentRepository enrollments, AssignmentRepository assignments,
                      ExamResultRepository examResults, AuthConfigRepository authConfig,
                      NotificationService notifications) {
        this.userService = userService;
        this.users = users;
        this.classrooms = classrooms;
        this.enrollments = enrollments;
        this.assignments = assignments;
        this.examResults = examResults;
        this.authConfig = authConfig;
        this.notifications = notifications;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (authConfig.findById(AuthConfig.SINGLETON_ID).isEmpty()) {
            AuthConfig config = new AuthConfig();
            config.setMode(AuthMode.S1); // baseline; an admin can switch to S2/S3 (UC-08)
            config.setOtpType("TOTP");
            config.setOtpValiditySeconds(90);
            authConfig.save(config);
        }

        if (users.count() > 0) {
            return;
        }
        log.info("Seeding AuthShield 360 lab data (simulated accounts only - BR-01)");

        UserResponse admin = userService.create(new CreateUserRequest(
                "admin01", "admin01@mailtrap.io", "0912000001", "Administrator", "admin123", RoleType.ADMIN, null));
        UserResponse teacher = userService.create(new CreateUserRequest(
                "teacher01", "teacher01@mailtrap.io", "0912000002", "Tung Nguyen", "teacher123", RoleType.TEACHER, null));
        UserResponse student1 = userService.create(new CreateUserRequest(
                "student01", "student01@mailtrap.io", "0912000003", "Minh Anh Tran", "student123", RoleType.STUDENT, null));
        UserResponse student2 = userService.create(new CreateUserRequest(
                "student02", "student02@mailtrap.io", "0912000004", "Gia Bao Le", "student123", RoleType.STUDENT, null));

        Classroom cs101 = new Classroom();
        cs101.setCode("CS101");
        cs101.setName("Introduction to Programming - CS101");
        cs101.setDescription("Sample class used for AuthShield 360 testing");
        cs101.setTeacherId(teacher.id());
        cs101 = classrooms.save(cs101);

        enrollments.save(new Enrollment(cs101.getId(), student1.id()));
        enrollments.save(new Enrollment(cs101.getId(), student2.id()));

        Instant now = Instant.now();

        Assignment onTime = new Assignment();
        onTime.setTitle("Assignment 1 - Loops and arrays");
        onTime.setDescription("Submit before the deadline to get full marks.");
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
        late.setTitle("Assignment 2 - Recursion (late submissions allowed)");
        late.setDescription("The deadline has passed but late submissions are still accepted with a penalty.");
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
        noLate.setTitle("Assignment 3 - Data structures (no late submissions)");
        noLate.setDescription("The deadline has passed and late submissions are not accepted.");
        noLate.setClassroomId(cs101.getId());
        noLate.setCreatedBy(teacher.id());
        noLate.setDueAt(now.minus(2, ChronoUnit.DAYS));
        noLate.setAllowLate(false);
        noLate.setAllowResubmission(true);
        noLate.setMaxAttempts(3);
        noLate.setStatus(AssignmentStatus.PUBLISHED);
        assignments.save(noLate);

        Assignment closed = new Assignment();
        closed.setTitle("Midterm exam (closed)");
        closed.setDescription("This exam is closed: students can neither submit nor update.");
        closed.setClassroomId(cs101.getId());
        closed.setCreatedBy(teacher.id());
        closed.setDueAt(now.minus(1, ChronoUnit.DAYS));
        closed.setAllowLate(false);
        closed.setAllowResubmission(false);
        closed.setMaxAttempts(1);
        closed.setStatus(AssignmentStatus.CLOSED);
        assignments.save(closed);

        Assignment resubmit = new Assignment();
        resubmit.setTitle("Assignment 4 - Resubmission allowed");
        resubmit.setDescription("You can resubmit and update the file multiple times before the deadline.");
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
        result.setSubject("Programming");
        result.setExamName("15-minute quiz");
        result.setScore(8.5);
        result.setMaxScore(10);
        result.setExamDate(LocalDate.now().minusDays(5));
        result.setRecordedBy(teacher.id());
        examResults.save(result);

        // Welcome notifications so the feature has content on first run.
        for (UserResponse u : List.of(admin, teacher, student1, student2)) {
            notifications.notifyUser(u.id(), u.role().name(), NotificationType.ACCOUNT_READY,
                    "Welcome to AuthShield 360",
                    "Your account is ready. Sign in with the password provided by your administrator.",
                    u.role() == RoleType.STUDENT ? "/student" : u.role() == RoleType.TEACHER ? "/teacher" : "/admin");
        }

        log.info("Seed complete. Accounts: admin01/admin123, teacher01/teacher123, student01|student02/student123");
    }
}

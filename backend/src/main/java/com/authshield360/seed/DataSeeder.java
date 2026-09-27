package com.authshield360.seed;

import com.authshield360.auth.AuthConfig;
import com.authshield360.auth.AuthConfigRepository;
import com.authshield360.auth.AuthMode;
import com.authshield360.config.AppProperties;
import com.authshield360.notification.NotificationService;
import com.authshield360.notification.NotificationType;
import com.authshield360.school.*;
import com.authshield360.security.CryptoService;
import com.authshield360.user.RoleType;
import com.authshield360.user.User;
import com.authshield360.user.UserRepository;
import com.authshield360.user.UserService;
import com.authshield360.user.UserStatus;
import com.authshield360.user.dto.CreateUserRequest;
import com.authshield360.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds simulated lab data only (BR-01): fake e-mails/phones, no real people.
 *
 * <p>Volume: 1 admin + 20 teachers + 20 students, plus dedicated <b>test fixtures</b> that give
 * every documented test case (see {@code docs/test-cases.md}) the exact data it needs:
 * a locked account, a disabled account, an MFA-enrolled account with a known TOTP secret, a
 * student enrolled in no class, and a set of CS101 assignments covering each submission rule
 * (on time, late allowed, late refused, cutoff passed, closed, resubmit allowed, single attempt).
 *
 * <p>Idempotent: runs only when there are no users yet.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final int TEACHER_COUNT = 20;
    private static final int STUDENT_COUNT = 20;

    /** Well-known demo secret (RFC 6238 sample) so testers can use a real authenticator app. */
    public static final String MFA_TEST_SECRET = "JBSWY3DPEHPK3PXP";

    private static final String[] TEACHER_NAMES = {
            "Tung Nguyen", "Lan Pham", "Minh Hoang", "Ha Vu", "Duc Tran",
            "Thao Le", "Quang Bui", "Nhung Do", "Hung Dang", "Linh Ho",
            "Tuan Phan", "Mai Ngo", "Khanh Dinh", "Yen Truong", "Son Lam",
            "Trang Vo", "Bao Mach", "Hieu Cao", "Ngoc Ta", "Phuc Chu"
    };

    private static final String[] STUDENT_NAMES = {
            "Minh Anh Tran", "Gia Bao Le", "An Nguyen", "Binh Pham", "Chi Hoang",
            "Dung Vu", "Em Tran", "Phuc Le", "Giang Bui", "Hoa Do",
            "Iris Dang", "Khoa Ho", "Lam Phan", "My Ngo", "Nam Dinh",
            "Oanh Truong", "Phong Lam", "Quynh Vo", "Son Mach", "Trang Cao"
    };

    private static final String[] SUBJECTS = {
            "Introduction to Programming", "Data Structures and Algorithms", "Database Systems",
            "Web Development", "Operating Systems", "Computer Networks",
            "Software Engineering", "Object-Oriented Design", "Discrete Mathematics",
            "Information Security"
    };

    private final UserService userService;
    private final UserRepository users;
    private final ClassroomRepository classrooms;
    private final EnrollmentRepository enrollments;
    private final AssignmentRepository assignments;
    private final AssignmentSubmissionRepository submissions;
    private final ExamResultRepository examResults;
    private final AuthConfigRepository authConfig;
    private final NotificationService notifications;
    private final CryptoService cryptoService;
    private final AppProperties props;

    public DataSeeder(UserService userService, UserRepository users, ClassroomRepository classrooms,
                      EnrollmentRepository enrollments, AssignmentRepository assignments,
                      AssignmentSubmissionRepository submissions, ExamResultRepository examResults,
                      AuthConfigRepository authConfig, NotificationService notifications,
                      CryptoService cryptoService, AppProperties props) {
        this.userService = userService;
        this.users = users;
        this.classrooms = classrooms;
        this.enrollments = enrollments;
        this.assignments = assignments;
        this.submissions = submissions;
        this.examResults = examResults;
        this.authConfig = authConfig;
        this.notifications = notifications;
        this.cryptoService = cryptoService;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAuthConfig();

        if (users.count() > 0) {
            return;
        }
        log.info("Seeding AuthShield 360 lab data: 1 admin + {} teachers + {} students + test fixtures "
                + "(simulated only - BR-01)", TEACHER_COUNT, STUDENT_COUNT);

        UserResponse admin = userService.create(new CreateUserRequest(
                "admin01", "admin01@mailtrap.io", "0912000001", "Administrator", "admin123", RoleType.ADMIN, null));
        List<UserResponse> teachers = createTeachers();
        List<UserResponse> students = createStudents();
        List<UserResponse> fixtures = createTestFixtures();

        List<Classroom> classes = createClassrooms(teachers);
        enrollStudents(students, classes);

        Classroom flagship = classes.get(0);
        List<Assignment> scenario = createFlagshipAssignments(flagship, teachers.get(0).id());
        List<Assignment> others = createOtherAssignments(teachers, classes);
        int autoSubmissions = createSubmissions(others);
        int scenarioSubmissions = createScenarioSubmissions(scenario, students);
        createExamResults(students, teachers);
        createNotifications(admin, teachers, students, fixtures);

        log.info("Seed complete -> 1 admin, {} teachers, {} students, {} test fixtures, {} classrooms, "
                        + "{} enrollments, {} assignments, {} submissions ({} scenario), {} exam results",
                teachers.size(), students.size(), fixtures.size(), classes.size(), enrollments.count(),
                scenario.size() + others.size(), autoSubmissions + scenarioSubmissions, scenarioSubmissions,
                examResults.count());
        log.info("Passwords -> admin01/admin123, teacher01..teacher20/teacher123, student01..student20/student123, "
                + "fixtures (locked01, disabled01, student_mfa01, student_lonely01)/student123");
        log.info("TOTP test secret for student_mfa01 = {} (RFC 6238 sample, lab only)", MFA_TEST_SECRET);
    }

    private void seedAuthConfig() {
        if (authConfig.findById(AuthConfig.SINGLETON_ID).isEmpty()) {
            AuthConfig config = new AuthConfig();
            config.setMode(AuthMode.S1); // baseline; an admin can switch to S2/S3 (UC-08)
            config.setOtpType("TOTP");
            config.setOtpValiditySeconds(90);
            config.setResendCooldownSeconds(60);
            config.setMaxResend(3);
            config.setMaxFailedAttempts(5);
            config.setRequireCaptchaAfter(3);
            authConfig.save(config);
        }
    }

    // ------------------------------------------------------------------ users

    private List<UserResponse> createTeachers() {
        List<UserResponse> created = new ArrayList<>();
        created.add(userService.create(new CreateUserRequest(
                "teacher01", "teacher01@mailtrap.io", phone(1), TEACHER_NAMES[0], "teacher123", RoleType.TEACHER, null)));
        for (int i = 2; i <= TEACHER_COUNT; i++) {
            created.add(userService.create(new CreateUserRequest(
                    String.format("teacher%02d", i), String.format("teacher%02d@mailtrap.io", i),
                    phone(1000 + i), TEACHER_NAMES[i - 1], "teacher123", RoleType.TEACHER, null)));
        }
        return created;
    }

    private List<UserResponse> createStudents() {
        List<UserResponse> created = new ArrayList<>();
        created.add(userService.create(new CreateUserRequest(
                "student01", "student01@mailtrap.io", phone(2000 + 1), STUDENT_NAMES[0], "student123", RoleType.STUDENT, null)));
        created.add(userService.create(new CreateUserRequest(
                "student02", "student02@mailtrap.io", phone(2000 + 2), STUDENT_NAMES[1], "student123", RoleType.STUDENT, null)));
        for (int i = 3; i <= STUDENT_COUNT; i++) {
            created.add(userService.create(new CreateUserRequest(
                    String.format("student%02d", i), String.format("student%02d@mailtrap.io", i),
                    phone(2000 + i), STUDENT_NAMES[i - 1], "student123", RoleType.STUDENT, null)));
        }
        return created;
    }

    /**
     * Accounts dedicated to specific test cases (see docs/test-cases.md §1.2).
     * They are normal accounts with a deliberately pre-set state.
     */
    private List<UserResponse> createTestFixtures() {
        List<UserResponse> created = new ArrayList<>();

        created.add(userService.create(new CreateUserRequest(
                "locked01", "locked01@mailtrap.io", "0915000001", "Locked Test Account", "student123",
                RoleType.STUDENT, null)));
        created.add(userService.create(new CreateUserRequest(
                "disabled01", "disabled01@mailtrap.io", "0915000002", "Disabled Test Account", "student123",
                RoleType.STUDENT, null)));
        created.add(userService.create(new CreateUserRequest(
                "student_mfa01", "mfa01@mailtrap.io", "0915000003", "MFA Enrolled Student", "student123",
                RoleType.STUDENT, null)));
        created.add(userService.create(new CreateUserRequest(
                "student_lonely01", "lonely01@mailtrap.io", "0915000004", "Student Without Class", "student123",
                RoleType.STUDENT, null)));

        // LOCKED: already past the failed-attempt threshold, locked for 15 minutes.
        User locked = users.findByUsername("locked01").orElseThrow();
        locked.setStatus(UserStatus.LOCKED);
        locked.setFailedAttempts(5);
        locked.setLockoutLevel(1);
        locked.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
        users.save(locked);

        // DISABLED: must be refused even with a correct password.
        User disabled = users.findByUsername("disabled01").orElseThrow();
        disabled.setStatus(UserStatus.DISABLED);
        users.save(disabled);

        // MFA enrolled with a KNOWN TOTP secret so a real authenticator app can be used.
        User mfa = users.findByUsername("student_mfa01").orElseThrow();
        mfa.setMfaEnabled(true);
        mfa.setMfaEnrolled(true);
        mfa.setMfaSecretEnc(cryptoService.encrypt(MFA_TEST_SECRET));
        users.save(mfa);

        return created;
    }

    private String phone(int i) {
        return String.format("0912%06d", i);
    }

    // -------------------------------------------------------------- classrooms

    private List<Classroom> createClassrooms(List<UserResponse> teachers) {
        List<Classroom> created = new ArrayList<>();
        for (int i = 1; i <= TEACHER_COUNT; i++) {
            String code = String.format("CS1%02d", i);
            Classroom classroom = new Classroom();
            classroom.setCode(code);
            classroom.setName(SUBJECTS[(i - 1) % SUBJECTS.length] + " - " + code);
            classroom.setDescription("Sample class #" + i + " for AuthShield 360 testing");
            classroom.setTeacherId(teachers.get(i - 1).id());
            created.add(classrooms.save(classroom));
        }
        return created;
    }

    private void enrollStudents(List<UserResponse> students, List<Classroom> classes) {
        for (int s = 0; s < students.size(); s++) {
            Long studentId = students.get(s).id();
            enrollments.save(new Enrollment(classes.get(s % classes.size()).getId(), studentId));
            enrollments.save(new Enrollment(classes.get((s + 1) % classes.size()).getId(), studentId));
        }
        // Keep the flagship class (CS101) with student01 + student02: every student scenario is
        // documented against CS101. student03 and later are NOT in CS101 -> "not enrolled" case.
        Long flagship = classes.get(0).getId();
        if (!enrollments.existsByClassroomIdAndStudentId(flagship, students.get(1).id())) {
            enrollments.save(new Enrollment(flagship, students.get(1).id()));
        }
    }

    // -------------------------------------------------------------- assignments

    /**
     * CS101 assignments, one per submission rule. Each is referenced by a test case:
     * A1 on time · A2 late allowed · A3 late refused · A4 resubmit allowed · A5 late window closed ·
     * A6 single attempt · A7 resubmission not allowed · A8 closed exam.
     */
    private List<Assignment> createFlagshipAssignments(Classroom flagship, Long teacherId) {
        Instant now = Instant.now();
        List<Assignment> created = new ArrayList<>();

        created.add(save(flagship, teacherId, "Assignment 1 - Loops and arrays",
                "Submit before the deadline to get full marks.",
                now.plus(7, ChronoUnit.DAYS), true, now.plus(10, ChronoUnit.DAYS), 10, true, 3, AssignmentStatus.PUBLISHED));

        created.add(save(flagship, teacherId, "Assignment 2 - Recursion (late submissions allowed)",
                "The deadline has passed but late submissions are still accepted with a penalty.",
                now.minus(2, ChronoUnit.DAYS), true, now.plus(5, ChronoUnit.DAYS), 10, true, 3, AssignmentStatus.PUBLISHED));

        created.add(save(flagship, teacherId, "Assignment 3 - Data structures (no late submissions)",
                "The deadline has passed and late submissions are not accepted.",
                now.minus(2, ChronoUnit.DAYS), false, null, 0, true, 3, AssignmentStatus.PUBLISHED));

        created.add(save(flagship, teacherId, "Assignment 4 - Resubmission allowed",
                "You can resubmit and update the file multiple times before the deadline.",
                now.plus(3, ChronoUnit.DAYS), true, now.plus(6, ChronoUnit.DAYS), 10, true, 5, AssignmentStatus.PUBLISHED));

        created.add(save(flagship, teacherId, "Assignment 5 - Late window closed",
                "Late submissions were allowed but the late window has now closed.",
                now.minus(3, ChronoUnit.DAYS), true, now.minus(1, ChronoUnit.DAYS), 10, true, 3, AssignmentStatus.PUBLISHED));

        created.add(save(flagship, teacherId, "Assignment 6 - Single attempt only",
                "Only one submission is accepted; the attempt limit is already reached for student02.",
                now.plus(2, ChronoUnit.DAYS), true, null, 0, true, 1, AssignmentStatus.PUBLISHED));

        created.add(save(flagship, teacherId, "Assignment 7 - Resubmission not allowed",
                "Submissions are accepted but the file cannot be updated afterwards.",
                now.plus(2, ChronoUnit.DAYS), true, null, 0, false, 3, AssignmentStatus.PUBLISHED));

        created.add(save(flagship, teacherId, "Midterm exam (closed)",
                "This exam is closed: students can neither submit nor update.",
                now.minus(1, ChronoUnit.DAYS), false, null, 0, false, 1, AssignmentStatus.CLOSED));

        return created;
    }

    private List<Assignment> createOtherAssignments(List<UserResponse> teachers, List<Classroom> classes) {
        Instant now = Instant.now();
        List<Assignment> created = new ArrayList<>();
        for (int i = 2; i <= classes.size(); i++) {
            Classroom classroom = classes.get(i - 1);
            Long owner = teachers.get(i - 1).id();

            created.add(save(classroom, owner, "Assignment 1 - " + shortSubject(i),
                    "Open assignment for demonstration data.",
                    now.plus(4 + (i % 7), ChronoUnit.DAYS), true, now.plus(9, ChronoUnit.DAYS), 10, true, 3,
                    AssignmentStatus.PUBLISHED));

            boolean allowLate = i % 2 == 0;
            created.add(save(classroom, owner, "Assignment 2 - " + shortSubject(i),
                    allowLate ? "Deadline passed; late submissions accepted with a penalty."
                            : "Deadline passed; late submissions are not accepted.",
                    now.minus(1 + (i % 5), ChronoUnit.DAYS), allowLate,
                    allowLate ? now.plus(5, ChronoUnit.DAYS) : null, allowLate ? 15 : 0, true, 3,
                    AssignmentStatus.PUBLISHED));
        }
        return created;
    }

    private String shortSubject(int i) {
        return SUBJECTS[(i - 1) % SUBJECTS.length];
    }

    private Assignment save(Classroom classroom, Long teacherId, String title, String description, Instant dueAt,
                            boolean allowLate, Instant lateCutoff, int penalty, boolean allowResubmission,
                            int maxAttempts, AssignmentStatus status) {
        Assignment a = new Assignment();
        a.setTitle(title);
        a.setDescription(description);
        a.setClassroomId(classroom.getId());
        a.setCreatedBy(teacherId);
        a.setDueAt(dueAt);
        a.setAllowLate(allowLate);
        a.setLateCutoffAt(lateCutoff);
        a.setLatePenaltyPct(penalty);
        a.setAllowResubmission(allowResubmission);
        a.setMaxAttempts(maxAttempts);
        a.setMaxScore(100);
        a.setStatus(status);
        return assignments.save(a);
    }

    // -------------------------------------------------------------- submissions

    /** Auto-generated submissions for the non-flagship classes only (keeps CS101 deterministic). */
    private int createSubmissions(List<Assignment> others) {
        int created = 0;
        Instant now = Instant.now();
        for (Assignment assignment : others) {
            if (assignment.getStatus() == AssignmentStatus.CLOSED) {
                continue;
            }
            List<Long> studentIds = enrollments.findByClassroomId(assignment.getClassroomId()).stream()
                    .map(Enrollment::getStudentId)
                    .toList();
            for (Long studentId : studentIds) {
                if ((assignment.getId() + studentId) % 5 == 0) {
                    continue; // this student did not submit
                }
                boolean late = assignment.getDueAt().isBefore(now);
                boolean graded = (assignment.getId() + studentId) % 3 == 0;
                created += storeSubmission(assignment, studentId, late, graded, 60, now).size();
            }
        }
        return created;
    }

    /**
     * Hand-written submissions for the CS101 scenario assignments so each documented case has a
     * known starting state. student01 index 0, student02 index 1, student03 index 2 (not enrolled).
     */
    private int createScenarioSubmissions(List<Assignment> scenario, List<UserResponse> students) {
        Long student01 = students.get(0).id();
        Long student02 = students.get(1).id();
        int created = 0;

        // A1 (on time): student02 already submitted; student01 has NOT (case S-01 is done live).
        created += create(scenario.get(0), student02, false, false, 0);

        // A2 (late allowed): student02 submitted late; student01 has not (case S-02 done live).
        created += create(scenario.get(1), student02, true, false, 0);

        // A3 (late refused): nobody submitted (case S-03 done live).
        // A8 (closed exam): nobody submitted (case S-05 done live).

        // A4 (resubmission allowed): student01 has 1 submission (case S-09 resubmits it);
        //     student02 has a submission that is already graded (case S-10 must be refused).
        created += create(scenario.get(3), student01, false, false, 0);
        created += create(scenario.get(3), student02, false, true, 85);

        // A5 (late window closed): nobody submitted (case S-04 done live).

        // A6 (single attempt, max=1): student02 already used the only attempt (case S-07).
        created += create(scenario.get(5), student02, false, false, 0);

        // A7 (resubmission not allowed): student02 already submitted (case S-08).
        created += create(scenario.get(6), student02, false, false, 0);

        return created;
    }

    private int create(Assignment assignment, Long studentId, boolean late, boolean graded, int score) {
        return storeSubmission(assignment, studentId, late, graded, score, Instant.now()).size();
    }

    private List<AssignmentSubmission> storeSubmission(Assignment assignment, Long studentId, boolean late,
                                                       boolean graded, int score, Instant now) {
        AssignmentSubmission s = new AssignmentSubmission();
        s.setAssignmentId(assignment.getId());
        s.setStudentId(studentId);
        s.setAttemptNumber(1);
        s.setOriginalName("submission-a" + assignment.getId() + "-u" + studentId + ".txt");
        s.setStoredName(storePlaceholder(assignment.getId(), studentId));
        s.setContentType("text/plain");
        s.setSizeBytes(64L);
        s.setSubmittedAt(late ? assignment.getDueAt().plus(3, ChronoUnit.HOURS)
                : assignment.getDueAt().minus(1, ChronoUnit.DAYS));
        s.setSubmissionStatus(late ? SubmissionStatus.LATE : SubmissionStatus.ON_TIME);
        if (graded) {
            s.setScore(score > 0 ? score : 60 + (int) ((assignment.getId() * 7 + studentId * 3) % 41));
            s.setFeedback("Reviewed - keep practising the exercises from the lecture notes.");
            s.setGradedBy(assignment.getCreatedBy());
            s.setGradedAt(s.getSubmittedAt().plus(2, ChronoUnit.DAYS));
        }
        return List.of(submissions.save(s));
    }

    /** Writes a tiny placeholder file so "Download" works for seeded submissions. */
    private String storePlaceholder(Long assignmentId, Long studentId) {
        String relative = "assignments/" + assignmentId + "/" + UUID.randomUUID() + "-s" + studentId + ".txt";
        try {
            Path target = Paths.get(props.getUploadDir()).toAbsolutePath().normalize().resolve(relative);
            Files.createDirectories(target.getParent());
            Files.writeString(target, "Seeded sample submission for assignment " + assignmentId
                    + " (student " + studentId + ").\n", StandardCharsets.UTF_8);
            return relative;
        } catch (IOException e) {
            log.warn("Could not write placeholder file for assignment {} student {}", assignmentId, studentId, e);
            return relative;
        }
    }

    // ------------------------------------------------------------- exam results

    private void createExamResults(List<UserResponse> students, List<UserResponse> teachers) {
        for (int i = 0; i < students.size(); i++) {
            UserResponse student = students.get(i);
            for (int k = 0; k < 2; k++) {
                ExamResult result = new ExamResult();
                result.setStudentId(student.id());
                result.setSubject(SUBJECTS[(i + k) % SUBJECTS.length]);
                result.setExamName(k == 0 ? "15-minute quiz" : "Midterm test");
                result.setScore(5.0 + ((i * 3 + k * 7) % 11) * 0.5); // 5.0 .. 10.0
                result.setMaxScore(10);
                result.setExamDate(LocalDate.now().minusDays(5L + i % 20));
                result.setRecordedBy(teachers.get(i % teachers.size()).id());
                examResults.save(result);
            }
        }
    }

    // ------------------------------------------------------------ notifications

    private void createNotifications(UserResponse admin, List<UserResponse> teachers,
                                     List<UserResponse> students, List<UserResponse> fixtures) {
        notifications.notifyUser(admin.id(), RoleType.ADMIN.name(), NotificationType.ACCOUNT_READY,
                "Welcome to AuthShield 360",
                "Your administrator account is ready. Review users, configuration and the audit log.",
                "/admin");

        for (UserResponse teacher : teachers) {
            notifications.notifyUser(teacher.id(), RoleType.TEACHER.name(), NotificationType.ACCOUNT_READY,
                    "Welcome to AuthShield 360",
                    "Your teaching account is ready. Create an assignment to notify your students.",
                    "/teacher");
        }
        for (UserResponse student : students) {
            notifications.notifyUser(student.id(), RoleType.STUDENT.name(), NotificationType.ACCOUNT_READY,
                    "Welcome to AuthShield 360",
                    "Your student account is ready. Check My assignments for upcoming work.",
                    "/student");
        }
        for (UserResponse fixture : fixtures) {
            notifications.notifyUser(fixture.id(), RoleType.STUDENT.name(), NotificationType.ACCOUNT_READY,
                    "Test fixture account ready",
                    "This account is pre-configured for a specific test case. See docs/test-cases.md.",
                    "/student");
        }

        // Graded / received notifications so the bell and the notification page have content.
        for (AssignmentSubmission s : submissions.findAll().stream().filter(x -> x.getScore() != null).limit(30).toList()) {
            Assignment assignment = assignments.findById(s.getAssignmentId()).orElse(null);
            if (assignment != null) {
                notifications.submissionGraded(assignment, s.getStudentId(), s.getScore(), assignment.getMaxScore());
            }
        }
        for (AssignmentSubmission s : submissions.findAll().stream().limit(20).toList()) {
            Assignment assignment = assignments.findById(s.getAssignmentId()).orElse(null);
            if (assignment == null) {
                continue;
            }
            String studentName = users.findById(s.getStudentId()).map(User::getFullName).orElse("A student");
            notifications.submissionReceived(assignment, studentName,
                    s.getSubmissionStatus() == SubmissionStatus.LATE, s.getAttemptNumber());
        }
    }
}

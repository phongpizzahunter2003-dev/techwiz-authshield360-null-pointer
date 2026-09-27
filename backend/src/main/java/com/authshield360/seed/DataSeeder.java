package com.authshield360.seed;

import com.authshield360.auth.AuthConfig;
import com.authshield360.auth.AuthConfigRepository;
import com.authshield360.auth.AuthMode;
import com.authshield360.config.AppProperties;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds simulated lab data only (BR-01): fake e-mails/phones, no real people.
 *
 * <p>Volume: 20 teacher accounts and 20 student accounts (20 sample records per group), plus
 * 20 classrooms, assignments, submissions, exam results and notifications so every list, chart
 * and pagination control has realistic content.
 *
 * <p>Idempotent: runs only when there are no users yet.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final int TEACHER_COUNT = 20;
    private static final int STUDENT_COUNT = 20;

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
    private final AppProperties props;

    public DataSeeder(UserService userService, UserRepository users, ClassroomRepository classrooms,
                      EnrollmentRepository enrollments, AssignmentRepository assignments,
                      AssignmentSubmissionRepository submissions, ExamResultRepository examResults,
                      AuthConfigRepository authConfig, NotificationService notifications, AppProperties props) {
        this.userService = userService;
        this.users = users;
        this.classrooms = classrooms;
        this.enrollments = enrollments;
        this.assignments = assignments;
        this.submissions = submissions;
        this.examResults = examResults;
        this.authConfig = authConfig;
        this.notifications = notifications;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAuthConfig();

        if (users.count() > 0) {
            return;
        }
        log.info("Seeding AuthShield 360 lab data: 1 admin + {} teachers + {} students (simulated only - BR-01)",
                TEACHER_COUNT, STUDENT_COUNT);

        UserResponse admin = userService.create(new CreateUserRequest(
                "admin01", "admin01@mailtrap.io", "0912000001", "Administrator", "admin123", RoleType.ADMIN, null));
        List<UserResponse> teachers = createTeachers();
        List<UserResponse> students = createStudents();
        List<Classroom> classes = createClassrooms(teachers);
        enrollStudents(students, classes);
        List<Assignment> allAssignments = createAssignments(teachers, classes);
        int submissionCount = createSubmissions(allAssignments);
        createExamResults(students, teachers);
        createNotifications(admin, teachers, students);

        log.info("Seed complete -> 1 admin, {} teachers, {} students, {} classrooms, {} enrollments, "
                        + "{} assignments, {} submissions, {} exam results",
                teachers.size(), students.size(), classes.size(), enrollments.count(),
                allAssignments.size(), submissionCount, examResults.count());
        log.info("Passwords -> admin01/admin123, teacher01..teacher20/teacher123, student01..student20/student123");
    }

    private void seedAuthConfig() {
        if (authConfig.findById(AuthConfig.SINGLETON_ID).isEmpty()) {
            AuthConfig config = new AuthConfig();
            config.setMode(AuthMode.S1); // baseline; an admin can switch to S2/S3 (UC-08)
            config.setOtpType("TOTP");
            config.setOtpValiditySeconds(90);
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
                    phone1(i), TEACHER_NAMES[i - 1], "teacher123", RoleType.TEACHER, null)));
        }
        return created;
    }

    private List<UserResponse> createStudents() {
        List<UserResponse> created = new ArrayList<>();
        created.add(userService.create(new CreateUserRequest(
                "student01", "student01@mailtrap.io", phone2(1), STUDENT_NAMES[0], "student123", RoleType.STUDENT, null)));
        created.add(userService.create(new CreateUserRequest(
                "student02", "student02@mailtrap.io", phone2(2), STUDENT_NAMES[1], "student123", RoleType.STUDENT, null)));
        for (int i = 3; i <= STUDENT_COUNT; i++) {
            created.add(userService.create(new CreateUserRequest(
                    String.format("student%02d", i), String.format("student%02d@mailtrap.io", i),
                    phone2(i), STUDENT_NAMES[i - 1], "student123", RoleType.STUDENT, null)));
        }
        return created;
    }

    private String phone(int i) {
        return String.format("09120000%02d", i);
    }

    private String phone1(int i) {
        return String.format("091200%04d", 1000 + i);
    }

    private String phone2(int i) {
        return String.format("091300%04d", 2000 + i);
    }

    // -------------------------------------------------------------- classrooms

    private List<Classroom> createClassrooms(List<UserResponse> teachers) {
        List<Classroom> created = new ArrayList<>();
        for (int i = 1; i <= TEACHER_COUNT; i++) {
            String subject = SUBJECTS[(i - 1) % SUBJECTS.length];
            Classroom classroom = new Classroom();
            classroom.setCode(String.format("CS1%02d", i));
            classroom.setName(subject + " - " + String.format("CS1%02d", i));
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
        // Keep the flagship class (CS1 01) with the two demo students for the IDOR/scenario tests.
        Long flagship = classes.get(0).getId();
        if (!enrollments.existsByClassroomIdAndStudentId(flagship, students.get(1).id())) {
            enrollments.save(new Enrollment(flagship, students.get(1).id()));
        }
    }

    // -------------------------------------------------------------- assignments

    private List<Assignment> createAssignments(List<UserResponse> teachers, List<Classroom> classes) {
        List<Assignment> created = new ArrayList<>();
        Instant now = Instant.now();

        // Classroom #1 keeps the hand-written scenarios used by the UC-A1..UC-A4 demo.
        Classroom flagship = classes.get(0);
        Long teacherId = teachers.get(0).id();
        created.add(save(flagship, teacherId, "Assignment 1 - Loops and arrays",
                "Submit before the deadline to get full marks.",
                now.plus(7, ChronoUnit.DAYS), true, now.plus(10, ChronoUnit.DAYS), 10, true, 3, AssignmentStatus.PUBLISHED));
        created.add(save(flagship, teacherId, "Assignment 2 - Recursion (late submissions allowed)",
                "The deadline has passed but late submissions are still accepted with a penalty.",
                now.minus(2, ChronoUnit.DAYS), true, now.plus(5, ChronoUnit.DAYS), 10, true, 3, AssignmentStatus.PUBLISHED));
        created.add(save(flagship, teacherId, "Assignment 3 - Data structures (no late submissions)",
                "The deadline has passed and late submissions are not accepted.",
                now.minus(2, ChronoUnit.DAYS), false, null, 0, true, 3, AssignmentStatus.PUBLISHED));
        created.add(save(flagship, teacherId, "Midterm exam (closed)",
                "This exam is closed: students can neither submit nor update.",
                now.minus(1, ChronoUnit.DAYS), false, null, 0, false, 1, AssignmentStatus.CLOSED));
        created.add(save(flagship, teacherId, "Assignment 4 - Resubmission allowed",
                "You can resubmit and update the file multiple times before the deadline.",
                now.plus(3, ChronoUnit.DAYS), true, now.plus(6, ChronoUnit.DAYS), 10, true, 5, AssignmentStatus.PUBLISHED));

        // Every other classroom gets two assignments: one open, one with a passed deadline.
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
                    now.minus(1 + (i % 5), ChronoUnit.DAYS), allowLate, allowLate ? now.plus(5, ChronoUnit.DAYS) : null,
                    allowLate ? 15 : 0, true, 3, AssignmentStatus.PUBLISHED));
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

    private int createSubmissions(List<Assignment> allAssignments) {
        int created = 0;
        Instant now = Instant.now();
        for (Assignment assignment : allAssignments) {
            if (assignment.getStatus() == AssignmentStatus.CLOSED) {
                continue; // closed exams stay empty so UC-A3 can be demonstrated
            }
            List<Long> studentIds = enrollments.findByClassroomId(assignment.getClassroomId()).stream()
                    .map(Enrollment::getStudentId)
                    .toList();
            for (Long studentId : studentIds) {
                // Deterministic "most students submit" pattern so the sample data is reproducible.
                if ((assignment.getId() + studentId) % 5 == 0) {
                    continue; // this student did not submit
                }
                boolean late = assignment.getDueAt().isBefore(now);
                boolean graded = (assignment.getId() + studentId) % 3 == 0;

                AssignmentSubmission submission = new AssignmentSubmission();
                submission.setAssignmentId(assignment.getId());
                submission.setStudentId(studentId);
                submission.setAttemptNumber(1);
                submission.setOriginalName("submission-" + assignment.getId() + "-" + studentId + ".txt");
                submission.setStoredName(storePlaceholder(assignment.getId(), studentId));
                submission.setContentType("text/plain");
                submission.setSizeBytes(64L);
                submission.setSubmittedAt(late ? assignment.getDueAt().plus(3, ChronoUnit.HOURS)
                        : assignment.getDueAt().minus(1, ChronoUnit.DAYS));
                submission.setSubmissionStatus(late ? SubmissionStatus.LATE : SubmissionStatus.ON_TIME);
                if (graded) {
                    submission.setScore(60 + (int) ((assignment.getId() * 7 + studentId * 3) % 41)); // 60..100
                    submission.setFeedback("Reviewed - keep practising the exercises from the lecture notes.");
                    submission.setGradedBy(assignment.getCreatedBy());
                    submission.setGradedAt(submission.getSubmittedAt().plus(2, ChronoUnit.DAYS));
                }
                submissions.save(submission);
                created++;
            }
        }
        return created;
    }

    /** Writes a tiny placeholder file so the "Download" action works for seeded submissions. */
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
                String subject = SUBJECTS[(i + k) % SUBJECTS.length];
                ExamResult result = new ExamResult();
                result.setStudentId(student.id());
                result.setSubject(subject);
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

    private void createNotifications(UserResponse admin, List<UserResponse> teachers, List<UserResponse> students) {
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

        // A few graded / received notifications so the bell and the notification page have content.
        List<AssignmentSubmission> graded = submissions.findAll().stream()
                .filter(s -> s.getScore() != null)
                .limit(30)
                .toList();
        for (AssignmentSubmission s : graded) {
            Assignment assignment = assignments.findById(s.getAssignmentId()).orElse(null);
            if (assignment == null) {
                continue;
            }
            notifications.submissionGraded(assignment, s.getStudentId(), s.getScore(), assignment.getMaxScore());
        }

        for (AssignmentSubmission s : submissions.findAll().stream().limit(20).toList()) {
            Assignment assignment = assignments.findById(s.getAssignmentId()).orElse(null);
            if (assignment == null) {
                continue;
            }
            String studentName = users.findById(s.getStudentId()).map(u -> u.getFullName()).orElse("A student");
            notifications.submissionReceived(assignment, studentName,
                    s.getSubmissionStatus() == SubmissionStatus.LATE, s.getAttemptNumber());
        }
    }
}

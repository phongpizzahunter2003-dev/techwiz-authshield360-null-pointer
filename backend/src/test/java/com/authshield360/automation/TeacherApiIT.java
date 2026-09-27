package com.authshield360.automation;

import com.authshield360.school.EnrollmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automation for docs/test-cases.md §4 (TEA-01..TEA-22): class and assignment management, grading
 * rules, data isolation and RBAC.
 */
@DisplayName("TEA — teacher portal (UC-05, UC-07, UC-A1..UC-A4)")
class TeacherApiIT extends AbstractApiIT {

    private static final String A1 = "Loops and arrays";

    @Autowired
    EnrollmentRepository enrollmentRepository;

    private String uniqueCode() {
        return "E2E" + Long.toString(System.nanoTime(), 36);
    }

    private Res createClass(String teacherToken, String code, String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("name", name);
        body.put("description", "created by the automation suite");
        return post(API + "/teacher/classrooms", body, teacherToken);
    }

    private Map<String, Object> createAssignment(String teacherToken, long classroomId, String title,
                                                 Instant dueAt, boolean allowLate, boolean allowResubmission) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "automation");
        body.put("classroomId", classroomId);
        body.put("dueAt", dueAt.toString());
        body.put("allowLate", allowLate);
        body.put("allowResubmission", allowResubmission);
        body.put("maxAttempts", 3);
        body.put("maxScore", 100);
        body.put("status", "PUBLISHED");
        Res r = post(API + "/teacher/assignments", body, teacherToken);
        assertThat(r.ok()).as("create assignment -> %s %s", r.status(), r.body()).isTrue();
        return r.data();
    }

    private long submissionIdFor(long assignmentId, long studentId, String teacherToken) {
        Res subs = get(API + "/teacher/assignments/" + assignmentId + "/submissions", teacherToken);
        assertThat(subs.ok()).isTrue();
        return subs.listData().stream()
                .filter(s -> ((Number) s.get("studentId")).longValue() == studentId)
                .map(s -> ((Number) s.get("id")).longValue())
                .max(Long::compareTo)
                .orElseThrow(() -> new AssertionError("no submission for student " + studentId));
    }

    // ------------------------------------------------------------------ classes

    @Test
    @DisplayName("TEA-01 · a teacher can create a class")
    void tea01CreateClass() {
        String code = uniqueCode();
        Res created = createClass(teacherToken(), code, "Automation Class " + code);
        assertThat(created.ok()).as("create class -> %s %s", created.status(), created.body()).isTrue();

        Res list = get(API + "/classrooms", teacherToken());
        assertThat(list.listData()).anyMatch(c -> code.equals(c.get("code")));

        if (created.data() != null && created.data().get("id") != null) {
            delete(API + "/teacher/classrooms/" + ((Number) created.data().get("id")).longValue(), teacherToken());
        }
    }

    @Test
    @DisplayName("TEA-02 · a duplicate class code is refused")
    void tea02DuplicateClassCode() {
        assertEnvelope(createClass(teacherToken(), "CS101", "Duplicate"), 409, "CONFLICT");
    }

    @Test
    @DisplayName("TEA-03 · a class without a name fails validation")
    void tea03ClassValidation() {
        assertEnvelope(createClass(teacherToken(), uniqueCode(), ""), 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("TEA-04 · enrolling a student works and notifies that student")
    void tea04EnrollStudent() {
        String code = uniqueCode();
        Map<String, Object> classroom = createClass(teacherToken(), code, "Enrol Class").data();
        long classroomId = ((Number) classroom.get("id")).longValue();
        Map<String, Object> temp = createTempStudent("enrol");
        try {
            String studentToken = token((String) temp.get("username"), "student123");
            assertThat(unreadCount(studentToken)).isZero();

            Res r = post(API + "/teacher/classrooms/" + classroomId + "/enroll",
                    Map.of("studentId", id(temp)), teacherToken());
            assertThat(r.ok()).as("enroll -> %s %s", r.status(), r.body()).isTrue();
            assertThat(unreadCount(studentToken)).isGreaterThan(0);
        } finally {
            delete(API + "/teacher/classrooms/" + classroomId, teacherToken());
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("TEA-05 · only student accounts can be enrolled")
    void tea05EnrollNonStudent() {
        long teacher02Id = user("teacher02").getId();
        assertEnvelope(post(API + "/teacher/classrooms/" + classId("CS101") + "/enroll",
                Map.of("studentId", teacher02Id), teacherToken()), 409, "CONFLICT");
    }

    @Test
    @DisplayName("TEA-06 · enrolling an already-enrolled student never creates a duplicate")
    void tea06EnrollIdempotent() {
        long cs101 = classId("CS101");
        long student01 = user("student01").getId();
        long before = enrollmentRepository.countByClassroomId(cs101);

        Res r = post(API + "/teacher/classrooms/" + cs101 + "/enroll",
                Map.of("studentId", student01), teacherToken());
        assertThat(r.status()).as("re-enroll -> %s %s", r.status(), r.body()).isIn(200, 409);
        assertThat(enrollmentRepository.countByClassroomId(cs101))
                .as("no duplicate enrolment").isEqualTo(before);
    }

    // ------------------------------------------------------------------ assignments

    @Test
    @DisplayName("TEA-07 · creating an assignment notifies the enrolled students")
    void tea07CreateAssignmentNotifies() {
        long before = unreadCount(studentToken("student01"));
        Map<String, Object> created = createAssignment(teacherToken(), classId("CS101"),
                "Automation assignment " + uniqueCode(), Instant.now().plusSeconds(86_400), true, true);
        assertThat(created.get("title")).isNotNull();
        assertThat(unreadCount(studentToken("student01"))).isGreaterThan(before);
    }

    @Test
    @DisplayName("TEA-08 · an assignment without a title or due date fails validation")
    void tea08AssignmentValidation() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("classroomId", classId("CS101"));
        body.put("allowLate", true);
        assertEnvelope(post(API + "/teacher/assignments", body, teacherToken()), 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("TEA-09 · maxScore below 1 fails validation")
    void tea09MinScore() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Bad score");
        body.put("classroomId", classId("CS101"));
        body.put("dueAt", Instant.now().plusSeconds(3_600).toString());
        body.put("maxScore", 0);
        assertEnvelope(post(API + "/teacher/assignments", body, teacherToken()), 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("TEA-10 · a past-due assignment that forbids late work blocks submissions")
    void tea10PastDueNoLate() {
        Map<String, Object> created = createAssignment(teacherToken(), classId("CS101"),
                "Past due " + uniqueCode(), Instant.now().minusSeconds(3_600), false, true);
        long assignmentId = ((Number) created.get("id")).longValue();

        Map<String, Object> temp = createTempStudent("pastdue");
        try {
            enroll(classId("CS101"), id(temp));
            String t = token((String) temp.get("username"), "student123");
            assertEnvelope(uploadText(API + "/student/assignments/" + assignmentId + "/submissions",
                    t, "answer.txt", "too late"), 409, "LATE_NOT_ALLOWED");
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("TEA-11 · a teacher can move an assignment deadline")
    void tea11UpdateDeadline() {
        Map<String, Object> created = createAssignment(teacherToken(), classId("CS101"),
                "Editable " + uniqueCode(), Instant.now().plusSeconds(3_600), true, true);
        long assignmentId = ((Number) created.get("id")).longValue();

        Res r = put(API + "/teacher/assignments/" + assignmentId,
                Map.of("dueAt", Instant.now().plusSeconds(172_800).toString()), teacherToken());
        assertThat(r.ok()).as("update -> %s %s", r.status(), r.body()).isTrue();
    }

    @Test
    @DisplayName("TEA-12 · closing an assignment locks further submissions")
    void tea12CloseAssignment() {
        Map<String, Object> created = createAssignment(teacherToken(), classId("CS101"),
                "Close me " + uniqueCode(), Instant.now().plusSeconds(86_400), true, true);
        long assignmentId = ((Number) created.get("id")).longValue();

        Map<String, Object> temp = createTempStudent("closed");
        try {
            enroll(classId("CS101"), id(temp));
            String t = token((String) temp.get("username"), "student123");

            Res closed = post(API + "/teacher/assignments/" + assignmentId + "/close", Map.of(), teacherToken());
            assertThat(closed.ok()).as("close -> %s %s", closed.status(), closed.body()).isTrue();
            assertThat(closed.data().get("status")).isEqualTo("CLOSED");

            assertEnvelope(uploadText(API + "/student/assignments/" + assignmentId + "/submissions",
                    t, "answer.txt", "after close"), 409, "SUBMISSION_LOCKED");
        } finally {
            deleteTempUser(temp);
        }
    }

    // ------------------------------------------------------------------ grading

    @Test
    @DisplayName("TEA-13 · grading a submission saves the score and notifies the student")
    void tea13GradeValid() {
        long assignment = assignmentId(A1);
        Map<String, Object> temp = createTempStudent("grade");
        try {
            enroll(classId("CS101"), id(temp));
            String studentToken = token((String) temp.get("username"), "student123");
            assertThat(uploadText(API + "/student/assignments/" + assignment + "/submissions",
                    studentToken, "answer.txt", "grade me").ok()).isTrue();

            long submissionId = submissionIdFor(assignment, id(temp), teacherToken());
            long beforeGrade = unreadCount(studentToken);
            Res graded = post(API + "/teacher/submissions/" + submissionId + "/grade",
                    Map.of("score", 90, "feedback", "well done"), teacherToken());
            assertThat(graded.ok()).as("grade -> %s %s", graded.status(), graded.body()).isTrue();
            assertThat(((Number) graded.data().get("score")).intValue()).isEqualTo(90);
            assertThat(unreadCount(studentToken)).isGreaterThan(beforeGrade);
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("TEA-14 · a score above maxScore is refused")
    void tea14GradeOverMax() {
        long assignment = assignmentId(A1);
        Map<String, Object> temp = createTempStudent("over");
        try {
            enroll(classId("CS101"), id(temp));
            String t = token((String) temp.get("username"), "student123");
            uploadText(API + "/student/assignments/" + assignment + "/submissions", t, "a.txt", "x");
            long submissionId = submissionIdFor(assignment, id(temp), teacherToken());
            assertEnvelope(post(API + "/teacher/submissions/" + submissionId + "/grade",
                    Map.of("score", 150), teacherToken()), 400, "VALIDATION_ERROR");
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("TEA-15 · a negative score is refused")
    void tea15GradeNegative() {
        long assignment = assignmentId(A1);
        Map<String, Object> temp = createTempStudent("neg");
        try {
            enroll(classId("CS101"), id(temp));
            String t = token((String) temp.get("username"), "student123");
            uploadText(API + "/student/assignments/" + assignment + "/submissions", t, "a.txt", "x");
            long submissionId = submissionIdFor(assignment, id(temp), teacherToken());
            assertEnvelope(post(API + "/teacher/submissions/" + submissionId + "/grade",
                    Map.of("score", -5), teacherToken()), 400, "VALIDATION_ERROR");
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("TEA-16 · a score exactly at maxScore is accepted (boundary)")
    void tea16GradeAtMax() {
        long assignment = assignmentId(A1);
        Map<String, Object> temp = createTempStudent("max");
        try {
            enroll(classId("CS101"), id(temp));
            String t = token((String) temp.get("username"), "student123");
            uploadText(API + "/student/assignments/" + assignment + "/submissions", t, "a.txt", "x");
            long submissionId = submissionIdFor(assignment, id(temp), teacherToken());
            Res r = post(API + "/teacher/submissions/" + submissionId + "/grade",
                    Map.of("score", 100), teacherToken());
            assertThat(r.ok()).as("boundary grade -> %s %s", r.status(), r.body()).isTrue();
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("TEA-17 · a teacher cannot grade another teacher's submission")
    void tea17OtherTeacherCannotGrade() {
        long assignment = assignmentId(A1);
        Map<String, Object> temp = createTempStudent("foreign");
        try {
            enroll(classId("CS101"), id(temp));
            String t = token((String) temp.get("username"), "student123");
            uploadText(API + "/student/assignments/" + assignment + "/submissions", t, "a.txt", "x");
            long submissionId = submissionIdFor(assignment, id(temp), teacherToken());

            String teacher02 = token("teacher02", "teacher123");
            assertEnvelope(post(API + "/teacher/submissions/" + submissionId + "/grade",
                    Map.of("score", 50), teacher02), 403, "ACCESS_DENIED");
        } finally {
            deleteTempUser(temp);
        }
    }

    // ------------------------------------------------------------------ isolation

    @Test
    @DisplayName("TEA-18 · a teacher only sees their own classes' assignments")
    void tea18OwnAssignmentsOnly() {
        Res r = get(API + "/assignments", token("teacher02", "teacher123"));
        assertThat(r.ok()).isTrue();
        assertThat(r.listData()).noneMatch(a -> String.valueOf(a.get("title")).contains(A1));
    }

    @Test
    @DisplayName("TEA-19 · the class roster lists the enrolled students")
    void tea19ClassRoster() {
        Res r = get(API + "/teacher/classrooms/" + classId("CS101") + "/students", teacherToken());
        assertThat(r.ok()).isTrue();
        assertThat(r.listData()).anyMatch(s -> "student01".equals(s.get("username")));
    }

    @Test
    @DisplayName("TEA-20 · a teacher cannot open a student who is not in their classes")
    void tea20StudentDetailIsolation() {
        String teacher02 = token("teacher02", "teacher123");
        long student03 = user("student03").getId();
        Res denied = get(API + "/teacher/students/" + student03, teacher02);
        assertThat(denied.status()).as("cross-teacher student detail -> %s %s", denied.status(), denied.body())
                .isIn(403, 404);

        long student01 = user("student01").getId();
        Res allowed = get(API + "/teacher/students/" + student01, teacherToken());
        assertThat(allowed.ok()).as("own student detail -> %s %s", allowed.status(), allowed.body()).isTrue();
    }

    @Test
    @DisplayName("TEA-21 · the teacher dashboard is scoped to the teacher")
    void tea21TeacherDashboard() {
        Res r = get(API + "/dashboard/teacher", teacherToken());
        assertThat(r.ok()).isTrue();
        assertThat(list(r.data().get("classrooms"))).isNotEmpty();
        assertThat(list(r.data().get("stats"))).isNotEmpty();
    }

    @Test
    @DisplayName("TEA-22 · a teacher cannot call the admin API")
    void tea22TeacherCannotCallAdminApi() {
        assertEnvelope(get(API + "/admin/config", teacherToken()), 403, "ACCESS_DENIED");
    }
}

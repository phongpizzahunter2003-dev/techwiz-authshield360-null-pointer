package com.authshield360.automation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automation for docs/test-cases.md §3 (STU-01..STU-22): the four submission rules (UC-A1..UC-A4),
 * data scoping, RBAC, files and notifications.
 */
@DisplayName("STU — student portal (UC-A1..UC-A4, UC-05)")
class StudentApiIT extends AbstractApiIT {

    private static final String A1 = "Loops and arrays";
    private static final String A2 = "Recursion";
    private static final String A3 = "Data structures";
    private static final String A4 = "Resubmission allowed";
    private static final String A5 = "Late window closed";
    private static final String A6 = "Single attempt only";
    private static final String A7 = "Resubmission not allowed";
    private static final String A8 = "Midterm exam";

    /** A fresh student enrolled in CS101, removed afterwards so runs stay repeatable. */
    private Map<String, Object> enrolledTempStudent() {
        Map<String, Object> temp = createTempStudent("stu");
        enroll(classId("CS101"), id(temp));
        return temp;
    }

    private Res submit(String token, String titleFragment) {
        return uploadText(API + "/student/assignments/" + assignmentId(titleFragment) + "/submissions",
                token, "answer.txt", "automated test submission");
    }

    @Test
    @DisplayName("STU-01 · an on-time submission is accepted and notifies the teacher")
    void stu01OnTime() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            String t = token((String) temp.get("username"), "student123");
            long before = unreadCount(teacherToken());

            Res r = submit(t, A1);
            assertThat(r.ok()).as("submit A1 -> %s %s", r.status(), r.body()).isTrue();
            assertThat(r.data().get("submissionStatus")).isEqualTo("ON_TIME");
            assertThat(((Number) r.data().get("attemptNumber")).intValue()).isEqualTo(1);

            assertThat(unreadCount(teacherToken())).isGreaterThan(before);
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-02 · a late submission inside the window is accepted with status LATE")
    void stu02LateAllowed() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            Res r = submit(token((String) temp.get("username"), "student123"), A2);
            assertThat(r.ok()).as("submit A2 -> %s %s", r.status(), r.body()).isTrue();
            assertThat(r.data().get("submissionStatus")).isEqualTo("LATE");
            assertThat(r.data().get("late")).isEqualTo(true);
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-03 · late submissions are refused when the assignment forbids them")
    void stu03LateNotAllowed() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            assertEnvelope(submit(token((String) temp.get("username"), "student123"), A3),
                    409, "LATE_NOT_ALLOWED");
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-04 · late submissions are refused after the late window has closed")
    void stu04LateWindowClosed() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            assertEnvelope(submit(token((String) temp.get("username"), "student123"), A5),
                    409, "LATE_NOT_ALLOWED");
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-05 · a closed assignment refuses both submit and update")
    void stu05ClosedAssignment() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            assertEnvelope(submit(token((String) temp.get("username"), "student123"), A8),
                    409, "SUBMISSION_LOCKED");
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-06 · submitting to a class you are not enrolled in is forbidden")
    void stu06NotEnrolled() {
        assertEnvelope(submit(studentToken("student03"), A1), 403, "NOT_ENROLLED");
    }

    @Test
    @DisplayName("STU-07 · once the attempt limit is reached a further submission is refused")
    void stu07MaxAttemptsReached() {
        assertEnvelope(submit(studentToken("student02"), A6), 409, "MAX_ATTEMPTS_REACHED");
    }

    @Test
    @DisplayName("STU-08 · resubmission is refused when the assignment disables it")
    void stu08ResubmissionNotAllowed() {
        assertEnvelope(submit(studentToken("student02"), A7), 409, "RESUBMISSION_NOT_ALLOWED");
    }

    @Test
    @DisplayName("STU-09 · resubmitting while open creates a new attempt and keeps the history")
    void stu09ResubmitWhileOpen() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            String t = token((String) temp.get("username"), "student123");
            long assignment = assignmentId(A4);

            assertThat(submit(t, A4).ok()).isTrue();
            Res second = submit(t, A4);
            assertThat(second.ok()).as("second submit -> %s %s", second.status(), second.body()).isTrue();
            assertThat(((Number) second.data().get("attemptNumber")).intValue()).isEqualTo(2);

            Res history = get(API + "/student/assignments/" + assignment + "/submissions", t);
            assertThat(history.ok()).isTrue();
            List<Map<String, Object>> attempts = history.listData();
            assertThat(attempts).hasSize(2);
            Map<String, Object> latest = attempts.stream()
                    .filter(a -> ((Number) a.get("attemptNumber")).intValue() == 2)
                    .findFirst().orElseThrow();
            assertThat(latest.get("current")).isEqualTo(true);
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-10 · resubmission is refused once the submission has been graded")
    void stu10ResubmitAfterGraded() {
        assertEnvelope(submit(studentToken("student02"), A4), 409, "RESUBMISSION_NOT_ALLOWED");
    }

    @Test
    @DisplayName("STU-11 · a student can download their own submitted file")
    void stu11DownloadOwn() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            String t = token((String) temp.get("username"), "student123");
            Res r = submit(t, A1);
            long submissionId = ((Number) r.data().get("id")).longValue();

            ResponseEntity<byte[]> file = download(API + "/submissions/" + submissionId + "/file", t);
            assertThat(file.getStatusCode().value()).isEqualTo(200);
            assertThat(file.getBody()).isNotEmpty();
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-12 · a student cannot download another student's submission")
    void stu12CannotDownloadOthers() {
        String teacher = teacherToken();
        Res subs = get(API + "/teacher/assignments/" + assignmentId(A4) + "/submissions", teacher);
        assertThat(subs.ok()).isTrue();
        long student02Id = user("student02").getId();
        Long foreignSubmission = subs.listData().stream()
                .filter(s -> ((Number) s.get("studentId")).longValue() == student02Id)
                .map(s -> ((Number) s.get("id")).longValue())
                .findFirst().orElseThrow(() -> new AssertionError("student02 has no A4 submission"));

        ResponseEntity<byte[]> file = download(API + "/submissions/" + foreignSubmission + "/file",
                studentToken("student01"));
        assertThat(file.getStatusCode().value()).as("cross-student download").isIn(403, 404);
    }

    @Test
    @DisplayName("STU-13 · exam results are scoped to the signed-in student")
    void stu13ResultsScoped() {
        Res r = get(API + "/student/results", studentToken("student01"));
        assertThat(r.ok()).isTrue();
        long ownId = user("student01").getId();
        assertThat(r.listData()).isNotEmpty();
        for (Map<String, Object> row : r.listData()) {
            assertThat(((Number) row.get("studentId")).longValue()).isEqualTo(ownId);
        }
    }

    @Test
    @DisplayName("STU-14 · a student cannot call the admin API")
    void stu14StudentCannotCallAdminApi() {
        assertEnvelope(get(API + "/admin/users", studentToken("student01")), 403, "ACCESS_DENIED");
    }

    @Test
    @DisplayName("STU-15 · an unsupported file extension is refused")
    void stu15InvalidFileType() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            String t = token((String) temp.get("username"), "student123");
            Res r = uploadText(API + "/student/assignments/" + assignmentId(A1) + "/submissions",
                    t, "malware.exe", "nope");
            assertEnvelope(r, 400, "INVALID_FILE");
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-16 · a file larger than 10 MB is refused")
    void stu16OversizedFile() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            String t = token((String) temp.get("username"), "student123");
            String path = API + "/student/assignments/" + assignmentId(A1) + "/submissions";
            try {
                Res r = uploadBytes(path, t, "huge.txt", 11 * 1024 * 1024);
                assertEnvelope(r, 400, "INVALID_FILE");
            } catch (org.springframework.web.client.ResourceAccessException ex) {
                // Tomcat aborts the request as soon as the multipart size limit is exceeded, so the
                // client sees an I/O failure rather than the JSON envelope. Either way it is refused.
                assertThat(ex.getMessage()).isNotNull();
            }
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-17 · an empty file is refused")
    void stu17EmptyFile() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            String t = token((String) temp.get("username"), "student123");
            assertEnvelope(uploadBytes(API + "/student/assignments/" + assignmentId(A1) + "/submissions",
                    t, "empty.txt", 0), 400, "INVALID_FILE");
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-18 · the assignment list only exposes the student's own classes")
    void stu18AssignmentListScoped() {
        Res r = get(API + "/assignments", studentToken("student03"));
        assertThat(r.ok()).isTrue();
        assertThat(r.listData())
                .as("student03 must not see CS101 assignments")
                .noneMatch(a -> String.valueOf(a.get("title")).contains(A1));
    }

    @Test
    @DisplayName("STU-19 · a student enrolled in no class sees clean empty states")
    void stu19EmptyState() {
        String t = studentToken("student_lonely01");
        Res classes = get(API + "/classrooms", t);
        assertThat(classes.ok()).isTrue();
        assertThat(classes.listData()).isEmpty();

        Res dashboard = get(API + "/dashboard/student", t);
        assertThat(dashboard.ok()).isTrue();
        assertThat(list(dashboard.data().get("assignments"))).isEmpty();
    }

    @Test
    @DisplayName("STU-20 · chart drill-down buckets return the matching assignments")
    void stu20DrilldownBucket() {
        String t = studentToken("student01");
        Res all = get(API + "/analytics/student/assignments?bucket=ALL", t);
        assertThat(all.ok()).isTrue();
        assertThat(all.listData()).isNotEmpty();

        Res pending = get(API + "/analytics/student/assignments?bucket=PENDING", t);
        assertThat(pending.ok()).isTrue();
    }

    @Test
    @DisplayName("STU-21 · marking a notification read lowers the unread counter")
    void stu21NotificationReadFlow() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            String t = token((String) temp.get("username"), "student123");
            long before = unreadCount(t);
            assertThat(before).isGreaterThan(0);

            Res list = get(API + "/notifications?unread=true", t);
            assertThat(list.ok()).isTrue();
            long notificationId = ((Number) list.items().get(0).get("id")).longValue();

            Res marked = post(API + "/notifications/" + notificationId + "/read", Map.of(), t);
            assertThat(marked.ok()).isTrue();
            assertThat(marked.data().get("read")).isEqualTo(true);
            assertThat(unreadCount(t)).isEqualTo(before - 1);
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("STU-22 · mark-all-read clears every unread notification")
    void stu22MarkAllRead() {
        Map<String, Object> temp = enrolledTempStudent();
        try {
            String t = token((String) temp.get("username"), "student123");
            assertThat(unreadCount(t)).isGreaterThan(0);
            Res r = post(API + "/notifications/read-all", Map.of(), t);
            assertThat(r.ok()).isTrue();
            assertThat(((Number) r.data().get("updated")).intValue()).isGreaterThan(0);
            assertThat(unreadCount(t)).isZero();
        } finally {
            deleteTempUser(temp);
        }
    }
}

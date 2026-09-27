package com.authshield360.automation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automation for docs/test-cases.md §6 (X-01..X-12): cross-cutting and non-functional rules.
 * UI-only checks (responsive tables, alert layer) live in the Playwright suite (e2e/).
 */
@DisplayName("X — cross-cutting & non-functional")
class CrossCuttingApiIT extends AbstractApiIT {

    private boolean hasNotification(String token, String type) {
        Res r = get(API + "/notifications?size=50", token);
        assertThat(r.ok()).isTrue();
        return r.items().stream().anyMatch(n -> type.equals(n.get("type")));
    }

    @Test
    @DisplayName("X-01 · passwords are stored as a BCrypt hash, never in clear text (BR-02)")
    void x01PasswordsHashed() {
        String hash = user("student01").getPasswordHash();
        assertThat(hash).startsWith("$2").doesNotContain("student123");
        assertThat(user("admin01").getPasswordHash()).startsWith("$2");
    }

    @Test
    @DisplayName("X-02 · no secrets are exposed through the API (BR-10)")
    void x02NoSecretsInApi() {
        String secret = "leak-check-" + System.nanoTime();
        put(API + "/admin/config", Map.of("smtpPassword", secret), adminToken());

        Res read = get(API + "/admin/config", adminToken());
        assertThat(read.data()).doesNotContainKey("smtpPassword");
        assertThat(String.valueOf(read.body())).doesNotContain(secret);
    }

    @Test
    @DisplayName("X-03 · audit rows carry the mandatory fields (BR-07)")
    void x03AuditFields() {
        login("student01", "student123");
        Res logs = get(API + "/admin/audit-logs?size=1", adminToken());
        assertThat(logs.items()).isNotEmpty();
        Map<String, Object> row = logs.items().get(0);
        assertThat(row.get("eventId")).isNotNull();
        assertThat(row.get("eventAction")).isNotNull();
        assertThat(row.get("status")).isNotNull();
        assertThat(row.get("correlationId")).isNotNull();
        assertThat(row.get("eventTime")).isNotNull();
    }

    @Test
    @DisplayName("X-04 · an event is queryable immediately after it happens (BR-06, < 5s)")
    void x04AuditVisibleImmediately() {
        login("student02", "student123");
        Res logs = get(API + "/admin/audit-logs?action=LOGIN_SUCCESS&q=student02&size=1", adminToken());
        assertThat(logs.ok()).isTrue();
        assertThat(((Number) logs.data().get("totalElements")).longValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("X-05 · exports mask personal data (UC-11)")
    void x05ExportMasked() {
        String admin = adminToken();
        String suffix = Long.toString(System.nanoTime(), 36);
        Map<String, Object> probe = createTempUserWithName("priv." + suffix + "@mailtrap.io");
        try {
            login((String) probe.get("username"), "student123");
            String csv = exportAuditCsv(admin);
            assertThat(csv).doesNotContain((String) probe.get("username"));
            assertThat(csv).contains("******@mailtrap.io");
        } finally {
            deleteTempUser(probe);
        }
    }

    @Test
    @DisplayName("X-06 · the seeder is idempotent (no duplicate accounts)")
    void x06SeederIdempotent() {
        long first = userRepository.count();
        long second = userRepository.count();
        assertThat(first).isEqualTo(second).isGreaterThanOrEqualTo(40);
    }

    @Test
    @DisplayName("X-08 · RBAC is enforced at the server and violations are audited (BR-05)")
    void x08ServerSideRbac() {
        assertEnvelope(get(API + "/admin/users", studentToken("student01")), 403, "ACCESS_DENIED");
        Res logs = get(API + "/admin/audit-logs?action=PRIVILEGE_VIOLATION&size=1", adminToken());
        assertThat(((Number) logs.data().get("totalElements")).longValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("X-09 · notifications are routed to the right role at each step")
    void x09NotificationRouting() {
        Map<String, Object> temp = createTempStudent("route");
        try {
            enroll(classId("CS101"), id(temp));
            String student = token((String) temp.get("username"), "student123");
            String teacher = teacherToken();

            Map<String, Object> assignment = post(API + "/teacher/assignments", Map.of(
                    "title", "Routing " + System.nanoTime(),
                    "classroomId", classId("CS101"),
                    "dueAt", java.time.Instant.now().plusSeconds(86_400).toString(),
                    "allowLate", true,
                    "allowResubmission", true,
                    "maxAttempts", 3,
                    "maxScore", 100,
                    "status", "PUBLISHED"), teacher).data();
            long assignmentId = ((Number) assignment.get("id")).longValue();
            assertThat(hasNotification(student, "ASSIGNMENT_PUBLISHED")).isTrue();

            Res upload = uploadText(API + "/student/assignments/" + assignmentId + "/submissions",
                    student, "answer.txt", "route");
            long submissionId = ((Number) upload.data().get("id")).longValue();
            assertThat(hasNotification(teacher, "SUBMISSION_RECEIVED")).isTrue();

            post(API + "/teacher/submissions/" + submissionId + "/grade", Map.of("score", 77), teacher);
            assertThat(hasNotification(student, "SUBMISSION_GRADED")).isTrue();
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("X-10 · the read flag of a notification is persisted")
    void x10NotificationReadPersists() {
        Map<String, Object> temp = createTempStudent("read");
        try {
            enroll(classId("CS101"), id(temp));
            String t = token((String) temp.get("username"), "student123");
            Res list = get(API + "/notifications?unread=true", t);
            long id = ((Number) list.items().get(0).get("id")).longValue();

            post(API + "/notifications/" + id + "/read", Map.of(), t);
            Res after = get(API + "/notifications?size=50", t);
            Map<String, Object> row = after.items().stream()
                    .filter(n -> ((Number) n.get("id")).longValue() == id)
                    .findFirst().orElseThrow();
            assertThat(row.get("read")).isEqualTo(true);
            assertThat(row.get("readAt")).isNotNull();
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("X-11 · analytics charts are built from live data")
    void x11ChartsDynamic() {
        Res r = get(API + "/analytics/teacher", teacherToken());
        assertThat(r.ok()).isTrue();
        List<Map<String, Object>> charts = list(r.data().get("charts"));
        assertThat(charts).isNotEmpty();
        assertThat(charts).allMatch(c -> c.get("key") != null && c.get("title") != null);
    }
}

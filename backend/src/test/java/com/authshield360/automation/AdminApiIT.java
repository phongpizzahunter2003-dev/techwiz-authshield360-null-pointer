package com.authshield360.automation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automation for docs/test-cases.md §5 (ADM-01..ADM-20): user & role administration, bulk auth-mode
 * assignment, configuration guard-rails, audit log query and export.
 */
@DisplayName("ADM — administrator portal (UC-07, UC-08, UC-11, UC-15, UC-16)")
class AdminApiIT extends AbstractApiIT {

    private Res users(int page, int size) {
        return get(API + "/admin/users?page=" + page + "&size=" + size, adminToken());
    }

    @Test
    @DisplayName("ADM-01 · the user list is paginated")
    void adm01ListUsersPaged() {
        Res first = users(0, 20);
        assertThat(first.ok()).isTrue();
        assertThat(first.items()).hasSize(20);
        assertThat(((Number) first.data().get("totalElements")).longValue()).isGreaterThanOrEqualTo(40);
        assertThat(((Number) first.data().get("totalPages")).intValue()).isGreaterThanOrEqualTo(2);

        Res second = users(1, 20);
        assertThat(second.items()).hasSize(20);
        assertThat(second.items().get(0).get("id")).isNotEqualTo(first.items().get(0).get("id"));
    }

    @Test
    @DisplayName("ADM-02 · an administrator can create a user")
    void adm02CreateUser() {
        Map<String, Object> temp = createTempStudent("adm");
        try {
            Res fetched = get(API + "/admin/users/" + id(temp), adminToken());
            assertThat(fetched.ok()).isTrue();
            assertThat(fetched.data().get("username")).isEqualTo(temp.get("username"));
            assertThat(fetched.data().get("role")).isEqualTo("STUDENT");
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("ADM-03 · a duplicate username is refused")
    void adm03DuplicateUsername() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "student01");
        body.put("email", "brand-new@mailtrap.io");
        body.put("password", "student123");
        body.put("role", "STUDENT");
        assertEnvelope(post(API + "/admin/users", body, adminToken()), 409, "USERNAME_EXISTS");
    }

    @Test
    @DisplayName("ADM-04 · an invalid e-mail fails validation")
    void adm04InvalidEmail() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "e2e" + System.nanoTime());
        body.put("email", "not-an-email");
        body.put("password", "student123");
        body.put("role", "STUDENT");
        assertEnvelope(post(API + "/admin/users", body, adminToken()), 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("ADM-05 · changing a role is audited as ROLE_ASSIGN")
    void adm05ChangeRole() {
        Map<String, Object> temp = createTempStudent("role");
        try {
            Res updated = put(API + "/admin/users/" + id(temp), Map.of("role", "TEACHER"), adminToken());
            assertThat(updated.ok()).as("role change -> %s %s", updated.status(), updated.body()).isTrue();
            assertThat(updated.data().get("role")).isEqualTo("TEACHER");

            Res logs = get(API + "/admin/audit-logs?action=ROLE_ASSIGN&size=1", adminToken());
            assertThat(logs.ok()).isTrue();
            assertThat(((Number) logs.data().get("totalElements")).longValue()).isGreaterThan(0);
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("ADM-06 · deleting a user removes it and is audited")
    void adm06DeleteUser() {
        Map<String, Object> temp = createTempUser("del", "ADMIN");
        long id = id(temp);
        assertThat(delete(API + "/admin/users/" + id, adminToken()).ok()).isTrue();
        assertThat(get(API + "/admin/users/" + id, adminToken()).status()).isEqualTo(404);

        Res logs = get(API + "/admin/audit-logs?action=USER_DELETE&size=1", adminToken());
        assertThat(((Number) logs.data().get("totalElements")).longValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("ADM-07 · resetting MFA clears the enrolment (UC-16)")
    void adm07ResetMfa() {
        var mfaUser = user("student_mfa01");
        long id = mfaUser.getId();
        String encryptedSecret = mfaUser.getMfaSecretEnc();
        assertThat(encryptedSecret).isNotNull();

        try {
            Res r = post(API + "/admin/users/" + id + "/reset-mfa", Map.of(), adminToken());
            assertThat(r.ok()).as("reset mfa -> %s %s", r.status(), r.body()).isTrue();
            assertThat(user("student_mfa01").isMfaEnrolled()).isFalse();
            assertThat(user("student_mfa01").getMfaSecretEnc()).isNull();
        } finally {
            // Restore the fixture so the TOTP login case keeps working in the same run.
            var u = user("student_mfa01");
            u.setMfaSecretEnc(encryptedSecret);
            u.setMfaEnrolled(true);
            u.setMfaEnabled(true);
            userRepository.save(u);
        }
    }

    @Test
    @DisplayName("ADM-08/09 · bulk auth-mode assignment applies to every user and can be reset")
    void adm08BulkApply() {
        String admin = adminToken(); // any apply-auth-mode changes admin01 too, so capture it first
        Res applied = post(API + "/admin/users/apply-auth-mode", Map.of("mode", "S2"), admin);
        assertThat(applied.ok()).as("bulk apply -> %s %s", applied.status(), applied.body()).isTrue();
        assertThat(((Number) applied.data().get("affected")).intValue()).isGreaterThanOrEqualTo(40);
        assertThat(user("student01").getAuthModeOverride()).isEqualTo("S2");

        Res reset = post(API + "/admin/users/apply-auth-mode", Map.of("mode", "INHERIT"), admin);
        assertThat(reset.ok()).isTrue();
        assertThat(user("student01").getAuthModeOverride()).isNull();
    }

    @Test
    @DisplayName("ADM-09b · an invalid mode is refused")
    void adm09InvalidBulkMode() {
        assertEnvelope(post(API + "/admin/users/apply-auth-mode", Map.of("mode", "S9"), adminToken()),
                400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("ADM-10 · the global mode can be switched and is persisted")
    void adm10ChangeGlobalMode() {
        String admin = adminToken(); // switching the global mode would break a fresh admin login
        Res updated = put(API + "/admin/config", Map.of("mode", "S3"), admin);
        assertThat(updated.ok()).as("config -> %s %s", updated.status(), updated.body()).isTrue();
        assertThat(updated.data().get("mode")).isEqualTo("S3");

        Res read = get(API + "/admin/config", admin);
        assertThat(read.data().get("mode")).isEqualTo("S3");

        Res logs = get(API + "/admin/audit-logs?action=CONFIG_CHANGE&size=1", admin);
        assertThat(((Number) logs.data().get("totalElements")).longValue()).isGreaterThan(0);

        put(API + "/admin/config", Map.of("mode", "S1"), admin);
    }

    @Test
    @DisplayName("ADM-11 · an out-of-range OTP validity is refused")
    void adm11OtpValidityRange() {
        assertEnvelope(put(API + "/admin/config", Map.of("otpValiditySeconds", 20), adminToken()),
                400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("ADM-12 · an out-of-range failed-attempt threshold is refused")
    void adm12MaxFailedRange() {
        assertEnvelope(put(API + "/admin/config", Map.of("maxFailedAttempts", 0), adminToken()),
                400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("ADM-13 · the SMTP password is stored encrypted and never returned")
    void adm13SmtpPasswordHidden() {
        String secret = "top-secret-smtp-" + System.nanoTime();
        Res saved = put(API + "/admin/config", Map.of("smtpPassword", secret), adminToken());
        assertThat(saved.ok()).as("save smtp -> %s %s", saved.status(), saved.body()).isTrue();
        assertThat(saved.data()).doesNotContainKey("smtpPassword");
        assertThat(saved.data().get("smtpPasswordSet")).isEqualTo(true);

        Res read = get(API + "/admin/config", adminToken());
        assertThat(read.data()).doesNotContainKey("smtpPassword");
        assertThat(String.valueOf(read.body())).doesNotContain(secret);

        String stored = config().getSmtpPasswordEnc();
        assertThat(stored).isNotNull().isNotEqualTo(secret);
        assertThat(cryptoService.decrypt(stored)).isEqualTo(secret);
    }

    @Test
    @DisplayName("ADM-14 · audit logs can be filtered and page size is capped at 50")
    void adm14AuditFilterAndCap() {
        Res filtered = get(API + "/admin/audit-logs?action=LOGIN_SUCCESS&size=50", adminToken());
        assertThat(filtered.ok()).isTrue();
        assertThat(filtered.items()).isNotEmpty();
        assertThat(filtered.items()).allMatch(row -> "LOGIN_SUCCESS".equals(row.get("eventAction")));

        Res capped = get(API + "/admin/audit-logs?size=500", adminToken());
        assertThat(capped.items().size()).isLessThanOrEqualTo(50);
    }

    @Test
    @DisplayName("ADM-15 · the CSV export masks e-mail and phone identifiers")
    void adm15AuditExportMasked() {
        String admin = adminToken();
        String suffix = Long.toString(System.nanoTime(), 36);
        Map<String, Object> emailProbe = createTempUserWithName("probe." + suffix + "@mailtrap.io");
        Map<String, Object> phoneProbe = createTempUserWithName(
                "0912" + String.format("%06d", Math.abs(System.nanoTime() % 1_000_000)));
        try {
            // Signing in records the identifier in the audit log, giving the export PII to mask.
            assertThat(login((String) emailProbe.get("username"), "student123").get("token")).isNotNull();
            assertThat(login((String) phoneProbe.get("username"), "student123").get("token")).isNotNull();

            String csv = exportAuditCsv(admin);
            assertThat(csv).doesNotContain((String) emailProbe.get("username"));
            assertThat(csv).contains("******@mailtrap.io");
            assertThat(csv).doesNotContain((String) phoneProbe.get("username"));
            assertThat(csv).contains("091***");
        } finally {
            deleteTempUser(emailProbe);
            deleteTempUser(phoneProbe);
        }
    }

    @Test
    @DisplayName("ADM-16 · the export reports the 10,000-row cap through headers")
    void adm16ExportCapHeaders() {
        ResponseEntity<byte[]> res = download(API + "/admin/audit-logs/export?format=csv", adminToken());
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getHeaders().getFirst("X-Export-Total")).isNotNull();
        assertThat(res.getHeaders().getFirst("X-Export-Truncated")).isIn("true", "false");
    }

    @Test
    @DisplayName("ADM-17 · the system page reports the database and row counts")
    void adm17DbStatus() {
        Res r = get(API + "/admin/system/db-status", adminToken());
        assertThat(r.ok()).isTrue();
        assertThat(String.valueOf(r.data().get("databaseProduct"))).containsIgnoringCase("H2");
        assertThat(r.data().get("persistent")).isEqualTo(false);
        assertThat(list(r.data().get("tables"))).isNotEmpty();
        assertThat(((Number) r.data().get("totalRows")).longValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("ADM-18 · the S1/S2/S3 comparison page returns all three modes")
    void adm18Comparison() {
        Res r = get(API + "/admin/comparison", adminToken());
        assertThat(r.ok()).isTrue();
        assertThat(list(r.data().get("modes"))).hasSize(3);
    }

    @Test
    @DisplayName("ADM-19 · the admin dashboard exposes stats and recent audit events")
    void adm19AdminDashboard() {
        Res r = get(API + "/dashboard/admin", adminToken());
        assertThat(r.ok()).isTrue();
        assertThat(list(r.data().get("stats"))).isNotEmpty();
        assertThat(list(r.data().get("recentEvents"))).isNotEmpty();
    }

    @Test
    @DisplayName("ADM-20 · non-admins are denied on every admin surface")
    void adm20NonAdminDenied() {
        String student = studentToken("student01");
        assertEnvelope(get(API + "/admin/users", student), 403, "ACCESS_DENIED");
        assertEnvelope(get(API + "/admin/config", student), 403, "ACCESS_DENIED");
        assertEnvelope(get(API + "/admin/audit-logs", student), 403, "ACCESS_DENIED");
        assertEnvelope(get(API + "/admin/system/db-status", student), 403, "ACCESS_DENIED");
    }
}

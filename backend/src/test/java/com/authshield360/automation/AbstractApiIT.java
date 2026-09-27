package com.authshield360.automation;

import com.authshield360.auth.AuthConfig;
import com.authshield360.auth.AuthConfigRepository;
import com.authshield360.auth.AuthMode;
import com.authshield360.auth.CaptchaService;
import com.authshield360.security.CryptoService;
import com.authshield360.security.SessionRecord;
import com.authshield360.security.SessionRepository;
import com.authshield360.security.TotpService;
import com.authshield360.user.User;
import com.authshield360.user.UserRepository;
import com.authshield360.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for the API automation suite (see docs/test-cases.md).
 *
 * <p>Runs the real Spring Boot application on a random port against the in-memory H2 `test`
 * profile and drives it over HTTP, exactly like the SPA does. The seeded lab fixtures
 * (locked01, disabled01, student_mfa01, student_lonely01, the CS101 scenario assignments) are the
 * data each test case is written against.
 *
 * <p>The auth configuration is restored to its documented S1 baseline before every test, and
 * {@code auth_mode_override} values are cleared, so tests are order-independent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
public abstract class AbstractApiIT {

    protected static final String API = "/api/v1";
    protected static final long CONFIG_ID = 1L; // AuthConfig.SINGLETON_ID
    protected static final String MFA_TEST_SECRET = "JBSWY3DPEHPK3PXP";

    /**
     * Accounts whose lock state is reset before every test. `locked01` and `disabled01` are
     * deliberately excluded because their pre-set state IS the fixture.
     */
    private static final List<String> RESETTABLE_ACCOUNTS = List.of(
            "admin01", "teacher01", "teacher02",
            "student01", "student02", "student03", "student11", "student12", "student13", "student14",
            "student_mfa01");

    @Autowired protected TestRestTemplate rest;
    @Autowired protected AuthConfigRepository configRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected SessionRepository sessionRepository;
    @Autowired protected CaptchaService captchaService;
    @Autowired protected TotpService totpService;
    @Autowired protected CryptoService cryptoService;

    // ------------------------------------------------------------------ config

    protected AuthConfig config() {
        return configRepository.findById(CONFIG_ID).orElseThrow();
    }

    protected AuthConfig mutateConfig(java.util.function.Consumer<AuthConfig> mutator) {
        AuthConfig c = config();
        mutator.accept(c);
        c.setUpdatedAt(Instant.now());
        c.setUpdatedBy("automation");
        return configRepository.save(c);
    }

    /** The configuration the fixtures were designed against (docs/test-cases.md §1.4). */
    protected void s1Baseline() {
        mutateConfig(c -> {
            c.setMode(AuthMode.S1);
            c.setOtpType("TOTP");
            c.setOtpLength(6);
            c.setOtpValiditySeconds(90);
            c.setResendCooldownSeconds(60);
            c.setMaxResend(3);
            c.setMaxFailedAttempts(5);
            c.setLockoutDurationsSeconds("60,300,900");
            c.setRequireCaptchaAfter(3);
            c.setEmailOtpEnabled(true);
        });
        clearOverrides();
    }

    protected void setMode(String mode) {
        mutateConfig(c -> c.setMode(AuthMode.valueOf(mode)));
    }

    /** Clear every per-user auth-mode override so the global mode always applies. */
    private void clearOverrides() {
        List<User> changed = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            if (u.getAuthModeOverride() != null) {
                u.setAuthModeOverride(null);
                changed.add(u);
            }
        }
        if (!changed.isEmpty()) {
            userRepository.saveAll(changed);
        }
    }

    @BeforeEach
    void restoreBaseline() {
        s1Baseline();
        for (String username : RESETTABLE_ACCOUNTS) {
            resetLoginState(username);
            // The captcha deque is a 120s rolling window kept in memory, so it must be cleared
            // between tests or repeated S2/S3 logins would start demanding a captcha.
            captchaService.reset(username);
        }
    }

    // ------------------------------------------------------------------ users

    protected User user(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                () -> new AssertionError("Seeded user not found: " + username));
    }

    /** Put an account back to a clean, unlocked, active state. */
    protected void resetLoginState(String username) {
        User u = user(username);
        u.setFailedAttempts(0);
        u.setLockoutLevel(0);
        u.setLockedUntil(null);
        u.setStatus(UserStatus.ACTIVE);
        userRepository.save(u);
    }

    /** Create a throwaway student (never touches the fixtures) and return the response data. */
    protected Map<String, Object> createTempStudent(String prefix) {
        return createTempUser(prefix, "STUDENT");
    }

    protected Map<String, Object> createTempUser(String prefix, String role) {
        String username = prefix + Long.toString(System.nanoTime(), 36);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("email", username + "@mailtrap.io");
        body.put("phone", "0900000000");
        body.put("fullName", "Temp " + username);
        body.put("password", "student123");
        body.put("role", role);
        body.put("authMode", "INHERIT");
        Res r = post(API + "/admin/users", body, adminToken());
        assertThat(r.ok()).as("create temp %s -> %s %s", role, r.status(), r.body()).isTrue();
        Map<String, Object> data = new LinkedHashMap<>(r.data());
        data.put("_password", "student123");
        return data;
    }

    protected long id(Map<String, Object> user) {
        return ((Number) user.get("id")).longValue();
    }

    protected void deleteTempUser(Map<String, Object> user) {
        delete(API + "/admin/users/" + id(user), adminToken());
    }

    /** Delete with a pre-captured admin token (needed when the test changed the global mode). */
    protected void deleteTempUser(Map<String, Object> user, String adminToken) {
        delete(API + "/admin/users/" + id(user), adminToken);
    }

    /** Create a user with an exact username (used by the PII-masking cases). */
    protected Map<String, Object> createTempUserWithName(String username) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("email", "probe" + System.nanoTime() + "@mailtrap.io");
        body.put("phone", "0900000000");
        body.put("fullName", "Probe " + username);
        body.put("password", "student123");
        body.put("role", "STUDENT");
        body.put("authMode", "INHERIT");
        Res r = post(API + "/admin/users", body, adminToken());
        assertThat(r.ok()).as("create probe %s -> %s %s", username, r.status(), r.body()).isTrue();
        Map<String, Object> data = new LinkedHashMap<>(r.data());
        data.put("_password", "student123");
        return data;
    }

    // ------------------------------------------------------------------ school

    protected Long classId(String code) {
        Res r = get(API + "/classrooms", adminToken());
        assertThat(r.ok()).isTrue();
        return r.listData().stream()
                .filter(c -> code.equals(c.get("code")))
                .map(c -> ((Number) c.get("id")).longValue())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Class not found: " + code));
    }

    protected Long assignmentId(String titleFragment) {
        Res r = get(API + "/assignments", adminToken());
        assertThat(r.ok()).isTrue();
        return r.listData().stream()
                .filter(a -> String.valueOf(a.get("title")).contains(titleFragment))
                .map(a -> ((Number) a.get("id")).longValue())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Assignment not found: " + titleFragment));
    }

    protected void enroll(Long classroomId, long studentId) {
        Res r = post(API + "/teacher/classrooms/" + classroomId + "/enroll",
                Map.of("studentId", studentId), teacherToken());
        assertThat(r.ok()).as("enroll -> %s %s", r.status(), r.body()).isTrue();
    }

    // ------------------------------------------------------------------ HTTP

    /** HTTP result plus the parsed ApiResponse envelope. */
    protected record Res(int status, Map<String, Object> body) {
        boolean ok() {
            return status >= 200 && status < 300;
        }

        String code() {
            return body == null ? null : (String) body.get("code");
        }

        String message() {
            return body == null ? null : (String) body.get("message");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data() {
            return body == null ? null : (Map<String, Object>) body.get("data");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listData() {
            return body == null ? null : (List<Map<String, Object>>) body.get("data");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items() {
            Map<String, Object> d = data();
            return d == null ? null : (List<Map<String, Object>>) d.get("items");
        }

        long number(String field) {
            return ((Number) data().get(field)).longValue();
        }
    }

    @SuppressWarnings("rawtypes")
    protected Res call(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        ResponseEntity<Map> res = rest.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
        return new Res(res.getStatusCode().value(), res.getBody());
    }

    protected Res get(String path, String token) {
        return call(HttpMethod.GET, path, null, token);
    }

    protected Res post(String path, Object body, String token) {
        return call(HttpMethod.POST, path, body, token);
    }

    protected Res put(String path, Object body, String token) {
        return call(HttpMethod.PUT, path, body, token);
    }

    protected Res delete(String path, String token) {
        return call(HttpMethod.DELETE, path, null, token);
    }

    // ------------------------------------------------------------------ auth

    protected Res loginRaw(String username, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        return post(API + "/auth/login", body, null);
    }

    protected Map<String, Object> login(String username, String password) {
        Res r = loginRaw(username, password);
        assertThat(r.ok()).as("login %s -> %s %s", username, r.status(), r.body()).isTrue();
        return r.data();
    }

    protected String token(String username, String password) {
        Object t = login(username, password).get("token");
        assertThat(t).as("token for " + username).isNotNull();
        return (String) t;
    }

    protected String adminToken() {
        return token("admin01", "admin123");
    }

    protected String teacherToken() {
        return token("teacher01", "teacher123");
    }

    protected String studentToken(String username) {
        return token(username, "student123");
    }

    protected Res verifyOtp(String challengeToken, String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("challengeToken", challengeToken);
        body.put("code", code);
        return post(API + "/auth/otp/verify", body, null);
    }

    protected Res resendOtp(String challengeToken) {
        return post(API + "/auth/resend-otp", Map.of("challengeToken", challengeToken), null);
    }

    protected long unreadCount(String token) {
        Res r = get(API + "/notifications/unread-count", token);
        assertThat(r.ok()).as("unread-count -> %s %s", r.status(), r.body()).isTrue();
        return ((Number) r.data().get("unread")).longValue();
    }

    // ------------------------------------------------------------------ files

    protected Res upload(String path, String token, String filename, byte[] content) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        ResponseEntity<Map> res = rest.exchange(path, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
        return new Res(res.getStatusCode().value(), res.getBody());
    }

    protected Res uploadText(String path, String token, String filename, String content) {
        return upload(path, token, filename, content.getBytes(StandardCharsets.UTF_8));
    }

    protected Res uploadBytes(String path, String token, String filename, int size) {
        return upload(path, token, filename, new byte[size]);
    }

    /** Raw bytes download (the file endpoint returns the file, not the JSON envelope). */
    protected ResponseEntity<byte[]> download(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    /** Download the audit CSV and return it as text. */
    protected String exportAuditCsv(String token) {
        ResponseEntity<byte[]> res = download(API + "/admin/audit-logs/export?format=csv", token);
        assertThat(res.getStatusCode().value()).as("export status").isEqualTo(200);
        return new String(res.getBody(), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ asserts

    protected void assertEnvelope(Res r, int status, String code) {
        assertThat(r.status()).as("HTTP status, body=%s", r.body()).isEqualTo(status);
        assertThat(r.code()).as("error code, body=%s", r.body()).isEqualTo(code);
    }

    protected Optional<SessionRecord> session(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> list(Object value) {
        return (List<Map<String, Object>>) Objects.requireNonNull(value);
    }
}

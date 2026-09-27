package com.authshield360.automation;

import com.authshield360.user.User;
import com.authshield360.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automation for docs/test-cases.md §2 (AUTH-01..AUTH-32): sign-in success/failure, the lockout
 * ladder, S2 Mobile OTP, S3 Email OTP, captcha anti-robot, TOTP/MFA enrolment, sessions.
 */
@DisplayName("AUTH — authentication & security (UC-01..UC-04, UC-06, UC-09, UC-10)")
class AuthApiIT extends AbstractApiIT {

    private static final Pattern CAPTCHA = Pattern.compile("(\\d+)\\s*\\+\\s*(\\d+)");

    // ---------------------------------------------------------------- S1 basics

    @Test
    @DisplayName("AUTH-01 · S1 sign-in succeeds for admin, teacher and student")
    void auth01S1LoginSuccess() {
        for (String[] account : new String[][]{
                {"admin01", "admin123"}, {"teacher01", "teacher123"}, {"student01", "student123"}}) {
            Map<String, Object> data = login(account[0], account[1]);
            assertThat(data.get("status")).as(account[0]).isEqualTo("AUTHENTICATED");
            assertThat(data.get("token")).as(account[0]).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) data.get("user");
            assertThat(summary.get("role")).as(account[0]).isNotNull();
        }
    }

    @Test
    @DisplayName("AUTH-02 · a wrong password returns the generic message and increments the counter")
    void auth02WrongPassword() {
        resetLoginState("student01");
        Res r = loginRaw("student01", "wrong-password-1");
        assertEnvelope(r, 401, "INVALID_CREDENTIALS");
        assertThat(r.message()).isEqualTo("Incorrect username or password. Please try again.");
        assertThat(user("student01").getFailedAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("AUTH-03 · four wrong passwords stay below the threshold (no lock)")
    void auth03BelowThreshold() {
        resetLoginState("student01");
        Res last = null;
        for (int i = 1; i <= 4; i++) {
            last = loginRaw("student01", "wrong-password-" + i);
        }
        assertEnvelope(last, 401, "INVALID_CREDENTIALS");
        User u = user("student01");
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(u.getFailedAttempts()).isEqualTo(4);
        assertThat(u.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("AUTH-04 · the fifth wrong password locks the account")
    void auth04FifthFailureLocks() {
        resetLoginState("student01");
        Res last = null;
        for (int i = 1; i <= 5; i++) {
            last = loginRaw("student01", "wrong-password-" + i);
        }
        assertEnvelope(last, 423, "ACCOUNT_LOCKED");
        assertThat(last.message()).contains("Please try again in");
        User u = user("student01");
        assertThat(u.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(u.getFailedAttempts()).isEqualTo(5);
        assertThat(u.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("AUTH-05 · a locked account is refused even with the correct password (fixture locked01)")
    void auth05LockedAccountRefused() {
        Res r = loginRaw("locked01", "student123");
        assertEnvelope(r, 423, "ACCOUNT_LOCKED");
        assertThat(r.message()).contains("Please try again in");
    }

    @Test
    @DisplayName("AUTH-06 · after the lockout window the next correct password succeeds and resets state")
    void auth06LockoutReleasedAfterExpiry() throws InterruptedException {
        mutateConfig(c -> {
            c.setMaxFailedAttempts(1);
            c.setLockoutDurationsSeconds("1,2,3");
        });
        Map<String, Object> temp = createTempStudent("lockrel");
        try {
            assertEnvelope(loginRaw((String) temp.get("username"), "wrong-one-1"), 423, "ACCOUNT_LOCKED");
            Thread.sleep(1_500);
            Map<String, Object> data = login((String) temp.get("username"), "student123");
            assertThat(data.get("status")).isEqualTo("AUTHENTICATED");
            User u = user((String) temp.get("username"));
            assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(u.getFailedAttempts()).isZero();
        } finally {
            deleteTempUser(temp);
        }
    }

    @Test
    @DisplayName("AUTH-07 · an unknown username gets the same generic message (no user enumeration)")
    void auth07UnknownUser() {
        Res r = loginRaw("no-such-user-42", "whatever-123");
        assertEnvelope(r, 401, "INVALID_CREDENTIALS");
        assertThat(r.message()).isEqualTo("Incorrect username or password. Please try again.");
    }

    @Test
    @DisplayName("AUTH-08 · a disabled account is refused before the password is checked (fixture disabled01)")
    void auth08DisabledAccount() {
        assertEnvelope(loginRaw("disabled01", "student123"), 403, "ACCOUNT_DISABLED");
        assertEnvelope(loginRaw("disabled01", "whatever-123"), 403, "ACCOUNT_DISABLED");
    }

    @Test
    @DisplayName("AUTH-09 · blank username / short password fail bean validation")
    void auth09Validation() {
        Res r = loginRaw("", "short");
        assertEnvelope(r, 400, "VALIDATION_ERROR");
        assertThat(r.data()).isNotEmpty();
    }

    // ---------------------------------------------------------------- S2 / OTP

    @Test
    @DisplayName("AUTH-10 · S2 with a correct password returns a Mobile OTP challenge")
    void auth10S2Challenge() {
        resetLoginState("student01");
        setMode("S2");
        Map<String, Object> data = login("student01", "student123");
        assertThat(data.get("status")).isEqualTo("OTP_REQUIRED");
        assertThat(data.get("factor")).isEqualTo("MOBILE_OTP");
        assertThat(data.get("challengeToken")).isNotNull();
        assertThat(data.get("deliveryCode")).isNotNull(); // expose-otp=true in the test profile
    }

    @Test
    @DisplayName("AUTH-11 · S2 with the correct OTP grants a session")
    void auth11S2CorrectOtp() {
        resetLoginState("student01");
        setMode("S2");
        Map<String, Object> data = login("student01", "student123");
        Res v = verifyOtp((String) data.get("challengeToken"), (String) data.get("deliveryCode"));
        assertThat(v.ok()).as("verify -> %s %s", v.status(), v.body()).isTrue();
        Map<String, Object> authed = v.data();
        assertThat(authed.get("status")).isEqualTo("AUTHENTICATED");
        String t = (String) authed.get("token");
        assertThat(get(API + "/auth/session", t).ok()).isTrue();
    }

    @Test
    @DisplayName("AUTH-12 · a wrong OTP is rejected and counts towards the lockout threshold")
    void auth12S2WrongOtp() {
        resetLoginState("student01");
        setMode("S2");
        Map<String, Object> data = login("student01", "student123");
        assertEnvelope(verifyOtp((String) data.get("challengeToken"), "000000"), 401, "OTP_INVALID");
        assertThat(user("student01").getFailedAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("AUTH-13 · an expired OTP is rejected with OTP_EXPIRED")
    void auth13S2ExpiredOtp() throws InterruptedException {
        resetLoginState("student01");
        mutateConfig(c -> {
            c.setMode(com.authshield360.auth.AuthMode.S2);
            c.setOtpValiditySeconds(1);
        });
        Map<String, Object> data = login("student01", "student123");
        Thread.sleep(1_500);
        assertEnvelope(verifyOtp((String) data.get("challengeToken"), (String) data.get("deliveryCode")),
                401, "OTP_EXPIRED");
    }

    @Test
    @DisplayName("AUTH-14 · five wrong OTPs lock the account (OTP failures count too)")
    void auth14OtpFailuresLock() {
        resetLoginState("student01");
        setMode("S2");
        Map<String, Object> data = login("student01", "student123");
        String challenge = (String) data.get("challengeToken");
        Res last = null;
        for (int i = 1; i <= 5; i++) {
            last = verifyOtp(challenge, "00000" + i);
        }
        assertEnvelope(last, 423, "ACCOUNT_LOCKED");
        assertThat(user("student01").getStatus()).isEqualTo(UserStatus.LOCKED);
    }

    @Test
    @DisplayName("AUTH-15 · resending inside the cooldown is throttled (429)")
    void auth15ResendThrottled() {
        resetLoginState("student01");
        setMode("S2");
        Map<String, Object> data = login("student01", "student123");
        assertEnvelope(resendOtp((String) data.get("challengeToken")), 429, "RESEND_THROTTLED");
    }

    @Test
    @DisplayName("AUTH-16 · after the cooldown a resend issues a new code and invalidates the old one")
    void auth16ResendReplacesCode() {
        resetLoginState("student01");
        mutateConfig(c -> {
            c.setMode(com.authshield360.auth.AuthMode.S2);
            c.setResendCooldownSeconds(0);
        });
        Map<String, Object> first = login("student01", "student123");
        String oldCode = (String) first.get("deliveryCode");

        Res resent = resendOtp((String) first.get("challengeToken"));
        assertThat(resent.ok()).as("resend -> %s %s", resent.status(), resent.body()).isTrue();
        String newCode = (String) resent.data().get("deliveryCode");
        assertThat(newCode).isNotEqualTo(oldCode);

        assertEnvelope(verifyOtp((String) resent.data().get("challengeToken"), oldCode), 401, "OTP_INVALID");
        assertThat(verifyOtp((String) resent.data().get("challengeToken"), newCode).ok()).isTrue();
    }

    @Test
    @DisplayName("AUTH-17 · the 4th resend exceeds maxResend (RESEND_LIMIT_EXCEEDED)")
    void auth17ResendLimit() {
        resetLoginState("student01");
        mutateConfig(c -> {
            c.setMode(com.authshield360.auth.AuthMode.S2);
            c.setResendCooldownSeconds(0);
            c.setMaxResend(3);
        });
        Map<String, Object> data = login("student01", "student123");
        String challenge = (String) data.get("challengeToken");
        for (int i = 1; i <= 3; i++) {
            Res ok = resendOtp(challenge);
            assertThat(ok.ok()).as("resend #%d -> %s %s", i, ok.status(), ok.body()).isTrue();
            challenge = (String) ok.data().get("challengeToken");
        }
        assertEnvelope(resendOtp(challenge), 429, "RESEND_LIMIT_EXCEEDED");
    }

    // ---------------------------------------------------------------- captcha

    @Test
    @DisplayName("AUTH-18 · exceeding requireCaptchaAfter raises a CAPTCHA_REQUIRED challenge")
    void auth18CaptchaRequired() {
        resetLoginState("student01");
        mutateConfig(c -> {
            c.setMode(com.authshield360.auth.AuthMode.S2);
            c.setRequireCaptchaAfter(0);
        });
        Res r = loginRaw("student01", "student123");
        assertEnvelope(r, 400, "CAPTCHA_REQUIRED");
        assertThat(r.data()).containsKeys("captchaChallengeId", "captchaQuestion");
        assertThat(String.valueOf(r.data().get("captchaQuestion"))).contains("+");
    }

    @Test
    @DisplayName("AUTH-19 · a wrong captcha answer is refused and a new challenge is issued")
    void auth19CaptchaWrongAnswer() {
        resetLoginState("student01");
        mutateConfig(c -> {
            c.setMode(com.authshield360.auth.AuthMode.S2);
            c.setRequireCaptchaAfter(0);
        });
        Res first = loginRaw("student01", "student123");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "student01");
        body.put("password", "student123");
        body.put("captchaChallengeId", first.data().get("captchaChallengeId"));
        body.put("captchaAnswer", "999999");
        Res second = post(API + "/auth/login", body, null);
        assertEnvelope(second, 400, "CAPTCHA_REQUIRED");
        assertThat(second.data().get("captchaChallengeId")).isNotEqualTo(first.data().get("captchaChallengeId"));
    }

    @Test
    @DisplayName("AUTH-20 · the correct captcha answer lets the OTP step proceed")
    void auth20CaptchaSolved() {
        resetLoginState("student01");
        mutateConfig(c -> {
            c.setMode(com.authshield360.auth.AuthMode.S2);
            c.setRequireCaptchaAfter(0);
        });
        Res first = loginRaw("student01", "student123");
        assertEnvelope(first, 400, "CAPTCHA_REQUIRED");
        Matcher m = CAPTCHA.matcher(String.valueOf(first.data().get("captchaQuestion")));
        assertThat(m.find()).isTrue();
        int answer = Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "student01");
        body.put("password", "student123");
        body.put("captchaChallengeId", first.data().get("captchaChallengeId"));
        body.put("captchaAnswer", String.valueOf(answer));
        Res second = post(API + "/auth/login", body, null);
        assertThat(second.ok()).as("captcha solved -> %s %s", second.status(), second.body()).isTrue();
        assertThat(second.data().get("status")).isEqualTo("OTP_REQUIRED");
    }

    // ---------------------------------------------------------------- S3

    @Test
    @DisplayName("AUTH-21/22 · S3 walks Mobile OTP then Email OTP and then grants the session")
    void auth21S3Flow() {
        resetLoginState("student01");
        setMode("S3");
        Map<String, Object> mobile = login("student01", "student123");
        assertThat(mobile.get("status")).isEqualTo("OTP_REQUIRED");
        assertThat(mobile.get("factor")).isEqualTo("MOBILE_OTP");

        Res afterMobile = verifyOtp((String) mobile.get("challengeToken"), (String) mobile.get("deliveryCode"));
        assertThat(afterMobile.ok()).isTrue();
        assertThat(afterMobile.data().get("status")).isEqualTo("OTP_REQUIRED");
        assertThat(afterMobile.data().get("factor")).isEqualTo("EMAIL_OTP");

        Res afterEmail = verifyOtp((String) afterMobile.data().get("challengeToken"),
                (String) afterMobile.data().get("deliveryCode"));
        assertThat(afterEmail.ok()).as("email verify -> %s %s", afterEmail.status(), afterEmail.body()).isTrue();
        assertThat(afterEmail.data().get("status")).isEqualTo("AUTHENTICATED");
    }

    @Test
    @DisplayName("AUTH-23 · a wrong Email OTP is rejected")
    void auth23S3WrongEmailOtp() {
        resetLoginState("student01");
        setMode("S3");
        Map<String, Object> mobile = login("student01", "student123");
        Res afterMobile = verifyOtp((String) mobile.get("challengeToken"), (String) mobile.get("deliveryCode"));
        assertEnvelope(verifyOtp((String) afterMobile.data().get("challengeToken"), "000000"), 401, "OTP_INVALID");
    }

    @Test
    @DisplayName("AUTH-24 · an expired Email OTP is rejected")
    void auth24S3ExpiredEmailOtp() throws InterruptedException {
        resetLoginState("student01");
        mutateConfig(c -> {
            c.setMode(com.authshield360.auth.AuthMode.S3);
            c.setOtpValiditySeconds(5);
        });
        Map<String, Object> mobile = login("student01", "student123");
        Res afterMobile = verifyOtp((String) mobile.get("challengeToken"), (String) mobile.get("deliveryCode"));
        assertThat(afterMobile.ok()).isTrue();
        Thread.sleep(5_500);
        assertEnvelope(verifyOtp((String) afterMobile.data().get("challengeToken"),
                (String) afterMobile.data().get("deliveryCode")), 401, "OTP_EXPIRED");
    }

    @Test
    @DisplayName("AUTH-25 · exceeding maxResend on the Email step is refused")
    void auth25S3ResendLimit() {
        resetLoginState("student01");
        mutateConfig(c -> {
            c.setMode(com.authshield360.auth.AuthMode.S3);
            c.setResendCooldownSeconds(0);
            c.setMaxResend(0);
        });
        Map<String, Object> mobile = login("student01", "student123");
        Res afterMobile = verifyOtp((String) mobile.get("challengeToken"), (String) mobile.get("deliveryCode"));
        assertEnvelope(resendOtp((String) afterMobile.data().get("challengeToken")),
                429, "RESEND_LIMIT_EXCEEDED");
    }

    // ---------------------------------------------------------------- TOTP / MFA

    @Test
    @DisplayName("AUTH-26 · the MFA fixture signs in with a real TOTP authenticator code")
    void auth26TotpLogin() {
        resetLoginState("student_mfa01");
        setMode("S2");
        Map<String, Object> data = login("student_mfa01", "student123");
        assertThat(data.get("status")).isEqualTo("OTP_REQUIRED");
        assertThat(data.get("totpBased")).isEqualTo(true);
        assertThat(data.get("deliveryCode")).isNull();
        assertThat(data.get("deliveryChannel")).isEqualTo("AUTHENTICATOR");

        Res v = verifyOtp((String) data.get("challengeToken"), totpService.currentCode(MFA_TEST_SECRET));
        assertThat(v.ok()).as("totp verify -> %s %s", v.status(), v.body()).isTrue();
        assertThat(v.data().get("status")).isEqualTo("AUTHENTICATED");
    }

    @Test
    @DisplayName("AUTH-27..29 · MFA enrolment: wrong code fails, correct code enables TOTP login")
    void auth27MfaEnrolment() {
        String admin = adminToken(); // capture before switching the global mode to S2
        Map<String, Object> temp = createTempStudent("mfau");
        try {
            String t = token((String) temp.get("username"), "student123");

            Res start = post(API + "/auth/mfa/enroll", Map.of(), t);
            assertThat(start.ok()).as("enroll start -> %s %s", start.status(), start.body()).isTrue();
            String secret = (String) start.data().get("secret");
            assertThat(secret).isNotBlank();
            assertThat(String.valueOf(start.data().get("otpauthUri"))).startsWith("otpauth://totp/");

            Res wrong = post(API + "/auth/mfa/enroll/confirm", Map.of("code", "000000"), t);
            assertEnvelope(wrong, 400, "INVALID_MFA_CODE");

            Res confirmed = post(API + "/auth/mfa/enroll/confirm",
                    Map.of("code", totpService.currentCode(secret)), t);
            assertThat(confirmed.ok()).as("confirm -> %s %s", confirmed.status(), confirmed.body()).isTrue();
            assertThat(user((String) temp.get("username")).isMfaEnrolled()).isTrue();

            setMode("S2");
            Map<String, Object> challenge = login((String) temp.get("username"), "student123");
            assertThat(challenge.get("totpBased")).isEqualTo(true);
            Res login = verifyOtp((String) challenge.get("challengeToken"), totpService.currentCode(secret));
            assertThat(login.ok()).isTrue();
            assertThat(login.data().get("status")).isEqualTo("AUTHENTICATED");
        } finally {
            deleteTempUser(temp, admin);
        }
    }

    // ---------------------------------------------------------------- sessions

    @Test
    @DisplayName("AUTH-30 · logout revokes the session and the old token is refused (replay)")
    void auth30LogoutAndReplay() {
        String t = token("student01", "student123");
        Res out = post(API + "/auth/logout", Map.of(), t);
        assertThat(out.ok()).as("logout -> %s %s", out.status(), out.body()).isTrue();
        assertEnvelope(get(API + "/auth/session", t), 401, "UNAUTHENTICATED");
    }

    @Test
    @DisplayName("AUTH-31 · a tampered token is refused")
    void auth31TamperedToken() {
        String t = token("student01", "student123");
        String tampered = t.substring(0, t.length() - 2) + (t.endsWith("aa") ? "bb" : "aa");
        Res r = get(API + "/auth/session", tampered);
        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("AUTH-32 · an expired server-side session is refused")
    void auth32SessionExpired() {
        String t = token("student01", "student123");
        Res session = get(API + "/auth/session", t);
        assertThat(session.ok()).isTrue();
        String sessionId = (String) session.data().get("sessionId");

        var record = session(sessionId).orElseThrow();
        record.setExpiresAt(Instant.now().minusSeconds(60));
        sessionRepository.save(record);

        Res r = get(API + "/auth/session", t);
        assertThat(r.status()).as("expired session -> %s %s", r.status(), r.body()).isEqualTo(401);
        // The filter cannot distinguish an expired session from a revoked one, so either code is valid.
        assertThat(r.code()).as("expired session code").isIn("SESSION_EXPIRED", "UNAUTHENTICATED");
    }

    @Test
    @DisplayName("AUTH-33 · a challenge token cannot be used as an access token")
    void auth33ChallengeTokenNotAnAccessToken() {
        resetLoginState("student01");
        setMode("S2");
        Map<String, Object> data = login("student01", "student123");
        String challenge = (String) data.get("challengeToken");
        Res r = get(API + "/auth/session", challenge);
        assertThat(r.status()).isEqualTo(401);
    }
}

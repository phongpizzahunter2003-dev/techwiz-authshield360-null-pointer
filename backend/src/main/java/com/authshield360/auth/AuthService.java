package com.authshield360.auth;

import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditEvent;
import com.authshield360.audit.AuditService;
import com.authshield360.auth.dto.*;
import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.security.*;
import com.authshield360.user.User;
import com.authshield360.user.UserRepository;
import com.authshield360.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Orchestrates the S1/S2/S3 login state machine (UC-01..UC-04), MFA enrollment (UC-09),
 * logout (UC-06) and session inspection.
 *
 * <p>Deliberately NOT {@code @Transactional}: each collaborator commits independently so that
 * failure counters and audit rows survive a rejected login.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SessionService sessionService;
    private final LockoutService lockoutService;
    private final OtpService otpService;
    private final CaptchaService captchaService;
    private final ConfigService configService;
    private final AuditService audit;
    private final TotpService totpService;
    private final CryptoService cryptoService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService,
                       SessionService sessionService, LockoutService lockoutService, OtpService otpService,
                       CaptchaService captchaService, ConfigService configService, AuditService audit,
                       TotpService totpService, CryptoService cryptoService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.lockoutService = lockoutService;
        this.otpService = otpService;
        this.captchaService = captchaService;
        this.configService = configService;
        this.audit = audit;
        this.totpService = totpService;
        this.cryptoService = cryptoService;
    }

    // ------------------------------------------------------------------ UC-01/02/03

    public LoginResponse login(LoginRequest req) {
        String identifier = req.username() == null ? "" : req.username().trim();
        String mode = configService.current().getMode().name();

        audit.record(AuditEvent.action(AuditAction.LOGIN_ATTEMPT).success()
                .user(identifier).factor("PASSWORD").mode(mode));

        User user = users.findByUsername(identifier).orElse(null);
        if (user == null) {
            audit.failure(AuditAction.LOGIN_FAIL, identifier, null, "PASSWORD", "INVALID_CREDENTIALS", null, mode);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Effective mode = per-user override (admin-assigned) or the global configuration.
        AuthMode effectiveMode = resolveMode(user);
        mode = effectiveMode.name();

        lockoutService.assertNotLocked(user);
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            audit.failure(AuditAction.LOGIN_FAIL, user.getUsername(), user.getRole().name(), "PASSWORD",
                    "INVALID_CREDENTIALS", null, mode);
            LockoutService.LockoutInfo info = lockoutService.recordFailure(user, "INVALID_CREDENTIALS", "PASSWORD");
            if (info.locked()) {
                throw lockedException(info.lockoutSeconds());
            }
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        AuthConfig config = configService.current();
        if (effectiveMode == AuthMode.S1) {
            return issueSession(user, "PASSWORD");
        }

        captchaService.recordOtpRequest(user.getUsername());
        requireCaptchaIfNeeded(user.getUsername(), req.captchaChallengeId(), req.captchaAnswer());

        OtpService.Challenge challenge = otpService.issue(user, OtpFactor.MOBILE_OTP);
        audit.record(AuditEvent.action(AuditAction.OTP_SENT).success()
                .user(user.getUsername()).role(user.getRole().name()).factor(OtpFactor.MOBILE_OTP.name())
                .mode(mode).detail("channel", challenge.deliveryChannel()));
        return otpRequired(user, OtpFactor.MOBILE_OTP, challenge, config);
    }

    // ------------------------------------------------------------------ UC-02/03/04

    public LoginResponse verifyOtp(OtpVerifyRequest req) {
        TokenClaims claims = parseChallenge(req.challengeToken());
        OtpFactor factor = OtpFactor.valueOf(claims.purpose());
        User user = users.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CHALLENGE));
        String mode = resolveMode(user).name();

        lockoutService.assertNotLocked(user);

        OtpService.Verification verification = otpService.verify(user, factor, req.code());
        if (!verification.valid()) {
            String reason = verification.expired() ? "EXPIRED_OTP" : "INVALID_OTP";
            String action = verification.expired()
                    ? (factor == OtpFactor.EMAIL_OTP ? AuditAction.EMAIL_OTP_EXPIRED : AuditAction.OTP_EXPIRED)
                    : (factor == OtpFactor.EMAIL_OTP ? AuditAction.EMAIL_OTP_FAILED : AuditAction.OTP_VERIFY_FAIL);
            audit.failure(action, user.getUsername(), user.getRole().name(), factor.name(), reason, null, mode);

            LockoutService.LockoutInfo info = lockoutService.recordFailure(user, reason, factor.name());
            if (info.locked()) {
                throw lockedException(info.lockoutSeconds());
            }
            throw verification.expired()
                    ? new BusinessException(ErrorCode.OTP_EXPIRED)
                    : new BusinessException(ErrorCode.OTP_INVALID);
        }

        audit.record(AuditEvent.action(AuditAction.OTP_VERIFY_SUCCESS).success()
                .user(user.getUsername()).role(user.getRole().name()).factor(factor.name()).mode(mode));

        AuthConfig config = configService.current();
        if (factor == OtpFactor.MOBILE_OTP && resolveMode(user) == AuthMode.S3) {
            if (!config.isEmailOtpEnabled()) {
                throw new BusinessException(ErrorCode.EMAIL_OTP_UNSUPPORTED);
            }
            OtpService.Challenge emailChallenge = otpService.issue(user, OtpFactor.EMAIL_OTP);
            audit.record(AuditEvent.action(AuditAction.EMAIL_OTP_SENT).success()
                    .user(user.getUsername()).role(user.getRole().name()).factor(OtpFactor.EMAIL_OTP.name()).mode(mode));
            return otpRequired(user, OtpFactor.EMAIL_OTP, emailChallenge, config);
        }

        return issueSession(user, factor.name());
    }

    public LoginResponse resendOtp(ResendOtpRequest req) {
        TokenClaims claims = parseChallenge(req.challengeToken());
        OtpFactor factor = OtpFactor.valueOf(claims.purpose());
        User user = users.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CHALLENGE));
        String mode = resolveMode(user).name();

        lockoutService.assertNotLocked(user);
        captchaService.recordOtpRequest(user.getUsername());
        requireCaptchaIfNeeded(user.getUsername(), req.captchaChallengeId(), req.captchaAnswer());

        audit.record(AuditEvent.action(AuditAction.RESEND_OTP_REQUEST).success()
                .user(user.getUsername()).role(user.getRole().name()).factor(factor.name()).mode(mode));

        try {
            OtpService.Challenge challenge = otpService.resend(user, factor);
            audit.record(AuditEvent.action(AuditAction.RESEND_OTP_SUCCESS).success()
                    .user(user.getUsername()).role(user.getRole().name()).factor(factor.name()).mode(mode));
            return otpRequired(user, factor, challenge, configService.current());
        } catch (OtpService.ThrottledException ex) {
            if (ex.getKind() == OtpService.ThrottleKind.LIMIT_EXCEEDED) {
                audit.failure(AuditAction.RESEND_OTP_LIMIT_EXCEEDED, user.getUsername(), user.getRole().name(),
                        factor.name(), "MAX_RESEND_EXCEEDED", null, mode);
                throw new BusinessException(ErrorCode.RESEND_LIMIT_EXCEEDED);
            }
            audit.failure(AuditAction.RESEND_OTP_THROTTLED, user.getUsername(), user.getRole().name(),
                    factor.name(), "TOO_MANY_REQUESTS", null, mode);
            throw new BusinessException(ErrorCode.RESEND_THROTTLED);
        }
    }

    // ------------------------------------------------------------------ UC-06

    public void logout() {
        SecurityUtils.currentOrEmpty().ifPresent(cu -> {
            sessionService.revoke(cu.sessionId(), "USER_LOGOUT");
            audit.record(AuditEvent.action(AuditAction.LOGOUT).success()
                    .user(cu.username()).role(cu.role()).session(cu.sessionId()));
        });
    }

    @Transactional(readOnly = true)
    public SessionResponse session() {
        CurrentUser cu = SecurityUtils.current();
        User user = users.findById(cu.userId()).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        var session = sessionService.find(cu.sessionId()).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        return new SessionResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getRole().name(), session.getSessionId(), session.getAuthMethod(),
                user.isMfaEnabled(), user.isMfaEnrolled(), user.getAuthModeOverride(),
                resolveMode(user).name(), session.getIssuedAt(), session.getExpiresAt());
    }

    // ------------------------------------------------------------------ UC-09

    @Transactional
    public MfaEnrollResponse enrollStart() {
        CurrentUser cu = SecurityUtils.current();
        User user = users.findById(cu.userId()).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        String secret = totpService.generateSecret();
        user.setMfaSecretEnc(cryptoService.encrypt(secret));
        user.setMfaEnabled(true);
        user.setMfaEnrolled(false);
        users.save(user);
        return new MfaEnrollResponse(secret, totpService.otpauthUri(secret, user.getUsername(), "AuthShield 360"));
    }

    @Transactional
    public void enrollConfirm(ConfirmMfaRequest req) {
        CurrentUser cu = SecurityUtils.current();
        User user = users.findById(cu.userId()).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        if (user.getMfaSecretEnc() == null) {
            throw new BusinessException(ErrorCode.MFA_NOT_ENROLLED);
        }
        String secret = cryptoService.decrypt(user.getMfaSecretEnc());
        if (!otpService.verifyEnrollmentSecret(secret, req.code())) {
            audit.failure(AuditAction.MFA_ENROLL_FAIL, user.getUsername(), "MOBILE_OTP", "INVALID_OTP", null);
            throw new BusinessException(ErrorCode.INVALID_MFA_CODE);
        }
        user.setMfaEnrolled(true);
        user.setMfaEnabled(true);
        users.save(user);
        audit.record(AuditEvent.action(AuditAction.MFA_ENROLL_SUCCESS).success()
                .user(user.getUsername()).role(user.getRole().name()).factor("MOBILE_OTP"));
    }

    // ------------------------------------------------------------------ helpers

    private LoginResponse issueSession(User user, String authMethod) {
        lockoutService.onSuccess(user);
        SessionRecord session = sessionService.issue(user.getId(), user.getUsername(), user.getRole().name(), authMethod);
        String token = tokenService.createAccessToken(user.getId(), user.getUsername(), user.getRole().name(),
                session.getSessionId());
        lockoutService.recordSuccess(user, authMethod);
        audit.record(AuditEvent.action(AuditAction.LOGIN_SUCCESS).success()
                .user(user.getUsername()).role(user.getRole().name()).factor(authMethod)
                .session(session.getSessionId()).mode(resolveMode(user).name()));
        return LoginResponse.authenticated(token, session.getExpiresAt(), summary(user));
    }

    private LoginResponse otpRequired(User user, OtpFactor factor, OtpService.Challenge challenge, AuthConfig config) {
        Duration ttl = Duration.ofSeconds(config.getOtpValiditySeconds());
        String challengeToken = tokenService.createChallengeToken(user.getId(), user.getUsername(), factor.name(), ttl);
        return LoginResponse.otpRequired(challengeToken, factor, ttl.toSeconds(),
                challenge.deliveryCode(), challenge.deliveryChannel(), challenge.totpBased());
    }

    private void requireCaptchaIfNeeded(String identifier, String challengeId, String answer) {
        if (!captchaService.requiresCaptcha(identifier)) {
            return;
        }
        try {
            captchaService.verify(challengeId, answer);
        } catch (BusinessException ex) {
            CaptchaService.Challenge challenge = captchaService.issue();
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("captchaChallengeId", challenge.id());
            fields.put("captchaQuestion", challenge.question());
            throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED, ErrorCode.CAPTCHA_REQUIRED.message(), fields);
        }
        captchaService.reset(identifier);
    }

    private TokenClaims parseChallenge(String challengeToken) {
        TokenClaims claims;
        try {
            claims = tokenService.parse(challengeToken);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.INVALID_CHALLENGE);
        }
        if (claims.purpose() == null || TokenClaims.PURPOSE_ACCESS.equals(claims.purpose())) {
            throw new BusinessException(ErrorCode.INVALID_CHALLENGE);
        }
        return claims;
    }

    private BusinessException lockedException(long seconds) {
        return new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                ErrorCode.ACCOUNT_LOCKED.message() + " Please try again in " + LockoutService.format(seconds) + ".");
    }

    public static UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getRole(), user.isMfaEnabled(), user.isMfaEnrolled());
    }

    /** Exposes an account's current failure state for dashboards/tests. */
    @Transactional(readOnly = true)
    public Instant lockedUntil(Long userId) {
        return users.findById(userId).map(User::getLockedUntil).orElse(null);
    }

    /**
     * Effective authentication mode for a user: the per-user override assigned by an
     * administrator, otherwise the global {@code auth_config} mode (UC-08).
     */
    public AuthMode resolveMode(User user) {
        String override = user.getAuthModeOverride();
        if (override != null && !override.isBlank()) {
            try {
                return AuthMode.valueOf(override.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fall through to the global configuration
            }
        }
        return configService.current().getMode();
    }
}

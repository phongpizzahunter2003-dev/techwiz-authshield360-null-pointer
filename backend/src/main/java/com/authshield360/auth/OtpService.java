package com.authshield360.auth;

import com.authshield360.config.AppProperties;
import com.authshield360.security.CryptoService;
import com.authshield360.security.TotpService;
import com.authshield360.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

/** Issues, throttles, resends and verifies OTP challenges (UC-02, UC-03, UC-04, UC-09). */
@Service
public class OtpService {

    private final OtpTokenRepository repository;
    private final TotpService totpService;
    private final CryptoService cryptoService;
    private final MailGateway mailGateway;
    private final ConfigService configService;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpTokenRepository repository, TotpService totpService, CryptoService cryptoService,
                      MailGateway mailGateway, ConfigService configService, AppProperties props) {
        this.repository = repository;
        this.totpService = totpService;
        this.cryptoService = cryptoService;
        this.mailGateway = mailGateway;
        this.configService = configService;
        this.props = props;
    }

    public record Challenge(OtpFactor factor, boolean totpBased, String deliveryCode, Instant expiresAt,
                            String deliveryChannel) { }

    public record Verification(boolean valid, boolean expired) { }

    /** First issuance of a factor challenge for a login flow. */
    @Transactional
    public Challenge issue(User user, OtpFactor factor) {
        AuthConfig config = configService.current();
        OtpToken token = repository.findByUserIdentifierAndFactor(user.getUsername(), factor)
                .orElseGet(OtpToken::new);
        token.setUserIdentifier(user.getUsername());
        token.setFactor(factor);

        boolean useTotp = factor == OtpFactor.MOBILE_OTP
                && user.isMfaEnrolled()
                && user.getMfaSecretEnc() != null
                && "TOTP".equalsIgnoreCase(config.getOtpType());
        token.setTotpBased(useTotp);
        token.setConsumed(false);
        token.setAttemptCount(0);
        token.setIssuedAt(Instant.now());
        token.setLastSentAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(config.getOtpValiditySeconds()));
        token.setResendCount(0);

        if (useTotp) {
            token.setCodeHash(null);
            token.setDeliveryChannel("AUTHENTICATOR");
            repository.save(token);
            return new Challenge(factor, true, null, token.getExpiresAt(), "AUTHENTICATOR");
        }

        String code = generateCode(config.getOtpLength());
        token.setCodeHash(hash(user.getUsername(), code));
        String channel = deliver(user, factor, code);
        token.setDeliveryChannel(channel);
        repository.save(token);
        return new Challenge(factor, false, expose(code), token.getExpiresAt(), channel);
    }

    /**
     * Resend handling (UC-04). Throws BusinessException on throttle/limit.
     * The controller maps throttle/limit to audit events.
     */
    @Transactional
    public Challenge resend(User user, OtpFactor factor) {
        AuthConfig config = configService.current();
        OtpToken token = repository.findByUserIdentifierAndFactor(user.getUsername(), factor).orElse(null);
        Instant now = Instant.now();

        if (token != null) {
            if (token.getLastSentAt() != null
                    && token.getLastSentAt().plusSeconds(config.getResendCooldownSeconds()).isAfter(now)) {
                throw new ThrottledException(ThrottleKind.THROTTLED, remainingSeconds(token, config, now));
            }
            if (token.getResendCount() >= config.getMaxResend()) {
                throw new ThrottledException(ThrottleKind.LIMIT_EXCEEDED, 0);
            }
        }
        // Reuse issue() to regenerate; preserves resendCount increment below.
        int previousResend = token == null ? 0 : token.getResendCount();
        Challenge challenge = issue(user, factor);
        token = repository.findByUserIdentifierAndFactor(user.getUsername(), factor).orElseThrow();
        token.setResendCount(previousResend + 1);
        repository.save(token);
        return challenge;
    }

    private long remainingSeconds(OtpToken token, AuthConfig config, Instant now) {
        return Math.max(0, token.getLastSentAt().plusSeconds(config.getResendCooldownSeconds()).getEpochSecond()
                - now.getEpochSecond());
    }

    /** Verifies a submitted code (single-use, time-limited, ±1 TOTP step). */
    @Transactional
    public Verification verify(User user, OtpFactor factor, String code) {
        OtpToken token = repository.findByUserIdentifierAndFactor(user.getUsername(), factor).orElse(null);
        Instant now = Instant.now();

        boolean useTotp = factor == OtpFactor.MOBILE_OTP && user.isMfaEnrolled() && user.getMfaSecretEnc() != null
                && "TOTP".equalsIgnoreCase(configService.current().getOtpType());

        if (useTotp) {
            String secret = cryptoService.decrypt(user.getMfaSecretEnc());
            boolean ok = totpService.verify(secret, code);
            if (ok && token != null) {
                token.setConsumed(true);
                repository.save(token);
            }
            return new Verification(ok, false);
        }

        if (token == null) {
            return new Verification(false, true);
        }
        if (token.getExpiresAt().isBefore(now)) {
            return new Verification(false, true);
        }
        if (token.isConsumed()) {
            return new Verification(false, true);
        }
        boolean ok = MessageDigest.isEqual(
                hash(user.getUsername(), code).getBytes(StandardCharsets.UTF_8),
                (token.getCodeHash() == null ? "" : token.getCodeHash()).getBytes(StandardCharsets.UTF_8));
        token.setAttemptCount(token.getAttemptCount() + 1);
        if (ok) {
            token.setConsumed(true);
        }
        repository.save(token);
        return new Verification(ok, false);
    }

    /** UC-09: verify a code during enrollment (validates against the pending secret). */
    public boolean verifyEnrollmentSecret(String secret, String code) {
        return totpService.verify(secret, code);
    }

    private String deliver(User user, OtpFactor factor, String code) {
        if (factor == OtpFactor.EMAIL_OTP) {
            String email = user.getEmail() == null ? "unknown" : user.getEmail();
            mailGateway.sendOtp(email, factor, code, "AuthShield 360 - Ma xac minh Email");
            return "EMAIL";
        }
        String phone = user.getPhone() == null || user.getPhone().isBlank() ? "unknown" : user.getPhone();
        mailGateway.sendOtp(phone, factor, code, "AuthShield 360 - Ma xac minh Mobile");
        return "SMS_SIMULATED";
    }

    private String expose(String code) {
        return props.isExposeOtp() ? code : null;
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String hash(String subject, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest((subject + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("OTP hashing failed", e);
        }
    }

    public enum ThrottleKind { THROTTLED, LIMIT_EXCEEDED }

    public static class ThrottledException extends RuntimeException {
        private final ThrottleKind kind;
        private final long retryAfterSeconds;

        public ThrottledException(ThrottleKind kind, long retryAfterSeconds) {
            super(kind == ThrottleKind.THROTTLED ? "TOO_MANY_REQUESTS" : "MAX_RESEND_EXCEEDED");
            this.kind = kind;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public ThrottleKind getKind() { return kind; }
        public long getRetryAfterSeconds() { return retryAfterSeconds; }
    }
}

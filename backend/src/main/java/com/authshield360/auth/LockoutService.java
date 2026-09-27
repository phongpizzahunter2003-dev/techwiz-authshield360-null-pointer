package com.authshield360.auth;

import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditEvent;
import com.authshield360.audit.AuditService;
import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.common.WebUtils;
import com.authshield360.user.User;
import com.authshield360.user.UserRepository;
import com.authshield360.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Failed-login protection with exponential backoff (UC-10, BR-04, VD-04).
 * {@code recordFailure} never throws, so the attempt counter and audit row always commit;
 * the caller decides which HTTP error to return.
 */
@Service
public class LockoutService {

    private final UserRepository users;
    private final LoginAttemptRepository attempts;
    private final ConfigService configService;
    private final AuditService audit;

    public LockoutService(UserRepository users, LoginAttemptRepository attempts,
                          ConfigService configService, AuditService audit) {
        this.users = users;
        this.attempts = attempts;
        this.configService = configService;
        this.audit = audit;
    }

    /** Outcome of a recorded failure. */
    public record LockoutInfo(boolean locked, long lockoutSeconds, int attemptCount) { }

    /** Throws when the account is currently locked (E1 of UC-01/UC-10). Read-only: rollback safe. */
    @Transactional(readOnly = true)
    public void assertNotLocked(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            long remaining = Duration.between(Instant.now(), user.getLockedUntil()).toSeconds();
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    ErrorCode.ACCOUNT_LOCKED.message() + " Please try again in " + format(remaining) + ".");
        }
    }

    /** Registers a failed attempt (wrong password OR wrong OTP - VD-04). Returns lockout info if triggered. */
    @Transactional
    public LockoutInfo recordFailure(User user, String reason, String factor) {
        attempts.save(new LoginAttempt(user.getUsername(), WebUtils.clientIp(), Instant.now(), false, reason));

        AuthConfig config = configService.current();
        int failed = user.getFailedAttempts() + 1;
        user.setFailedAttempts(failed);

        if (failed >= config.getMaxFailedAttempts()) {
            long[] ladder = config.lockoutLadder();
            int level = Math.min(user.getLockoutLevel() + 1, ladder.length);
            long seconds = ladder[level - 1];
            user.setLockoutLevel(level);
            user.setLockedUntil(Instant.now().plusSeconds(seconds));
            user.setStatus(UserStatus.LOCKED);
            users.save(user);
            audit.record(AuditEvent.action(AuditAction.LOCKOUT_TRIGGERED).failure("EXCEEDED_MAX_ATTEMPTS")
                    .user(user.getUsername()).role(user.getRole().name()).factor(factor)
                    .detail("attemptCount", failed).detail("lockoutDuration", seconds));
            return new LockoutInfo(true, seconds, failed);
        }
        users.save(user);
        return new LockoutInfo(false, 0, failed);
    }

    /** Clears the counter after a fully successful login and reports lockout release. */
    @Transactional
    public void onSuccess(User user) {
        if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null || user.getStatus() == UserStatus.LOCKED) {
            user.setFailedAttempts(0);
            user.setLockoutLevel(0);
            user.setLockedUntil(null);
            user.setStatus(UserStatus.ACTIVE);
            users.save(user);
            audit.record(AuditEvent.action(AuditAction.LOCKOUT_RELEASED).success()
                    .user(user.getUsername()).role(user.getRole().name()));
        }
    }

    @Transactional
    public void recordSuccess(User user, String factor) {
        attempts.save(new LoginAttempt(user.getUsername(), WebUtils.clientIp(), Instant.now(), true, null));
    }

    public static String format(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}

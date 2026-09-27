package com.authshield360.auth;

import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight bot challenge (functional spec UC-02/UC-03: "captcha khi request OTP liên tục").
 * Tracks rapid OTP requests per identifier; when the configured threshold is exceeded, the caller
 * must solve an arithmetic challenge before a new OTP is issued.
 */
@Service
public class CaptchaService {

    private static final long WINDOW_SECONDS = 120;
    private static final long CHALLENGE_TTL_SECONDS = 180;

    private final ConfigService configService;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();
    private final Map<String, Stored> challenges = new ConcurrentHashMap<>();

    public CaptchaService(ConfigService configService) {
        this.configService = configService;
    }

    public record Challenge(String id, String question, Instant expiresAt) { }

    private record Stored(String answer, Instant expiresAt) { }

    public void recordOtpRequest(String identifier) {
        Deque<Instant> deque = requestLog.computeIfAbsent(identifier, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(Instant.now());
            prune(deque);
        }
    }

    public boolean requiresCaptcha(String identifier) {
        Deque<Instant> deque = requestLog.get(identifier);
        if (deque == null) return false;
        synchronized (deque) {
            prune(deque);
            return deque.size() > configService.current().getRequireCaptchaAfter();
        }
    }

    /** Clears the request counter after a captcha is solved successfully. */
    public void reset(String identifier) {
        Deque<Instant> deque = requestLog.get(identifier);
        if (deque != null) {
            synchronized (deque) {
                deque.clear();
            }
        }
    }

    public Challenge issue() {
        int a = 3 + random.nextInt(9);
        int b = 2 + random.nextInt(8);
        String id = UUID.randomUUID().toString();
        String question = "Hãy tính: " + a + " + " + b + " = ?";
        Instant expiresAt = Instant.now().plusSeconds(CHALLENGE_TTL_SECONDS);
        challenges.put(id, new Stored(String.valueOf(a + b), expiresAt));
        return new Challenge(id, question, expiresAt);
    }

    /** Verifies and consumes a challenge; throws CAPTCHA_REQUIRED when missing/incorrect/expired. */
    public void verify(String challengeId, String answer) {
        if (challengeId == null || answer == null) {
            throw required();
        }
        Stored stored = challenges.remove(challengeId);
        if (stored == null || stored.expiresAt().isBefore(Instant.now())
                || !stored.answer().equals(answer.trim())) {
            throw required();
        }
    }

    private BusinessException required() {
        return new BusinessException(ErrorCode.CAPTCHA_REQUIRED);
    }

    private void prune(Deque<Instant> deque) {
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
        }
    }
}

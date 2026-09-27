package com.authshield360.security;

import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.config.AppProperties;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Compact HMAC-SHA256 signed token service (architecture.md AD-2).
 * Format: base64url(header).base64url(payload).base64url(signature).
 * No external JWT library required; the session record remains the authority (AD-1).
 */
@Service
public class TokenService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final AppProperties props;
    private final ObjectMapper objectMapper;

    public TokenService(AppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public String createAccessToken(Long userId, String username, String role, String sessionId) {
        return create(userId, username, role, TokenClaims.PURPOSE_ACCESS, sessionId,
                Duration.ofMinutes(props.getTokenTtlMinutes()));
    }

    public String createChallengeToken(Long userId, String username, String factor, Duration ttl) {
        return create(userId, username, null, factor, null, ttl);
    }

    private String create(Long userId, String username, String role, String purpose, String sessionId, Duration ttl) {
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", userId);
        payload.put("usr", username);
        if (role != null) payload.put("role", role);
        payload.put("purpose", purpose);
        if (sessionId != null) payload.put("sid", sessionId);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plus(ttl).getEpochSecond());
        payload.put("jti", UUID.randomUUID().toString());
        try {
            String header = ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String body = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signingInput = header + "." + body;
            String signature = ENCODER.encodeToString(hmac(signingInput));
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create token", e);
        }
    }

    /** Parses and verifies a token; throws {@link BusinessException} when invalid/expired. */
    @SuppressWarnings("unchecked")
    public TokenClaims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = hmac(signingInput);
        byte[] actual;
        try {
            actual = DECODER.decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), Map.class);
            Instant exp = Instant.ofEpochSecond(((Number) payload.get("exp")).longValue());
            if (exp.isBefore(Instant.now())) {
                throw new BusinessException(ErrorCode.SESSION_EXPIRED);
            }
            return new TokenClaims(
                    ((Number) payload.get("sub")).longValue(),
                    (String) payload.get("usr"),
                    (String) payload.get("role"),
                    (String) payload.get("purpose"),
                    (String) payload.get("sid"),
                    (String) payload.get("jti"),
                    Instant.ofEpochSecond(((Number) payload.get("iat")).longValue()),
                    exp);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.getSigningKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }
}

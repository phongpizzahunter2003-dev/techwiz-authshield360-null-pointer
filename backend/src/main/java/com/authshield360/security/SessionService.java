package com.authshield360.security;

import com.authshield360.common.WebUtils;
import com.authshield360.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Issues, validates and revokes server-side sessions (UC-06, BR-08). */
@Service
public class SessionService {

    private final SessionRepository repository;
    private final AppProperties props;

    public SessionService(SessionRepository repository, AppProperties props) {
        this.repository = repository;
        this.props = props;
    }

    @Transactional
    public SessionRecord issue(Long userId, String username, String role, String authMethod) {
        SessionRecord s = new SessionRecord();
        s.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        s.setUserId(userId);
        s.setUsername(username);
        s.setRole(role);
        s.setAuthMethod(authMethod);
        s.setIssuedAt(Instant.now());
        s.setLastSeenAt(Instant.now());
        s.setExpiresAt(Instant.now().plusSeconds(props.getSessionTimeoutMinutes() * 60));
        s.setClientIp(WebUtils.clientIp());
        s.setUserAgent(WebUtils.userAgent());
        s.setActive(true);
        return repository.save(s);
    }

    @Transactional(readOnly = true)
    public Optional<SessionRecord> find(String sessionId) {
        if (sessionId == null) return Optional.empty();
        return repository.findBySessionId(sessionId);
    }

    @Transactional
    public void touch(String sessionId) {
        if (sessionId != null) repository.touch(sessionId, Instant.now());
    }

    @Transactional
    public void revoke(String sessionId, String reason) {
        repository.findBySessionId(sessionId).ifPresent(s -> {
            s.setActive(false);
            s.setRevokedAt(Instant.now());
            s.setRevokeReason(reason);
            repository.save(s);
        });
    }

    @Transactional
    public int revokeAllForUser(Long userId, String reason) {
        return repository.revokeAllForUser(userId, Instant.now(), reason);
    }

    @Transactional
    public void expire(String sessionId) {
        repository.findBySessionId(sessionId).ifPresent(s -> {
            s.setActive(false);
            s.setRevokeReason("SESSION_TIMEOUT");
            repository.save(s);
        });
    }
}

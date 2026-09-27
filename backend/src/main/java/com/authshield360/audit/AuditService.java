package com.authshield360.audit;

import com.authshield360.common.Correlation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central audit sink. Writes happen inside the caller's transaction so events are visible
 * within the 5-second NFR (BR-06). Secrets are scrubbed by {@link MaskingUtil} (BR-02/BR-10).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final MaskingUtil masking;

    public AuditService(AuditLogRepository repository, MaskingUtil masking) {
        this.repository = repository;
        this.masking = masking;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent event) {
        AuditLog entity = event.toEntity(masking);
        try {
            repository.save(entity);
        } catch (RuntimeException ex) {
            // Auditing must never break a business flow; surface to log only.
            log.error("Failed to persist audit event {} (correlation={})", entity.getEventAction(),
                    Correlation.current(), ex);
        }
        log.info("AUDIT {}{} user={} role={} factor={} status={}{}",
                entity.getEventAction(),
                entity.getFailureReason() != null ? "(" + entity.getFailureReason() + ")" : "",
                entity.getUserIdentifier(), entity.getRole(), entity.getAuthFactor(), entity.getStatus(),
                Correlation.current() != null ? " cid=" + Correlation.current() : "");
    }

    /** Convenience: successful event. */
    public void success(String action, String userIdentifier, String role, String factor, String sessionId) {
        record(AuditEvent.action(action).success().user(userIdentifier).role(role).factor(factor).session(sessionId));
    }

    /** Convenience: failed event. */
    public void failure(String action, String userIdentifier, String role, String factor,
                        String reason, String sessionId) {
        record(AuditEvent.action(action).failure(reason).user(userIdentifier).role(role)
                .factor(factor).session(sessionId));
    }

    /** Records a failed event with extra detail. */
    public void failure(String action, String userIdentifier, String factor, String reason,
                        java.util.Map<String, Object> detail) {
        AuditEvent event = AuditEvent.action(action).failure(reason).user(userIdentifier).factor(factor);
        if (detail != null) {
            detail.forEach(event::detail);
        }
        record(event);
    }

    /** Records a failed event tagged with the active auth mode (UC-15 comparison). */
    public void failure(String action, String userIdentifier, String role, String factor,
                        String reason, String sessionId, String mode) {
        record(AuditEvent.action(action).failure(reason).user(userIdentifier).role(role)
                .factor(factor).session(sessionId).mode(mode));
    }
}

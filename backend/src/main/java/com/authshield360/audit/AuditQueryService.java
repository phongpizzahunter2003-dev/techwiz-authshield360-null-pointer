package com.authshield360.audit;

import com.authshield360.audit.dto.AuditLogResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Read side of the audit log: filtering and pagination (UC-11). */
@Service
public class AuditQueryService {

    private final AuditLogRepository repository;

    public AuditQueryService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> page(AuditFilter filter, Pageable pageable) {
        return repository.findAll(specification(filter), pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> list(AuditFilter filter, Pageable pageable) {
        return repository.findAll(specification(filter), pageable).getContent();
    }

    @Transactional(readOnly = true)
    public long count(AuditFilter filter) {
        return repository.count(specification(filter));
    }

    @Transactional(readOnly = true)
    public AuditLog byId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new com.authshield360.common.BusinessException(
                        com.authshield360.common.ErrorCode.NOT_FOUND));
    }

    public AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getEventTime(), log.getEventId(), log.getEventAction(),
                log.getStatus(), log.getAuthFactor(), log.getAuthMode(), log.getUserIdentifier(), log.getRole(),
                log.getClientIp(), log.getSessionId(), log.getFailureReason(), log.getCorrelationId(),
                log.getDetail());
    }

    static Specification<AuditLog> specification(AuditFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.role() != null && !filter.role().isBlank()) {
                predicates.add(cb.equal(root.get("role"), filter.role()));
            }
            if (filter.action() != null && !filter.action().isBlank()) {
                predicates.add(cb.equal(root.get("eventAction"), filter.action()));
            }
            if (filter.status() != null && !filter.status().isBlank()) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.mode() != null && !filter.mode().isBlank()) {
                predicates.add(cb.equal(root.get("authMode"), filter.mode()));
            }
            if (filter.query() != null && !filter.query().isBlank()) {
                String like = "%" + filter.query().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("userIdentifier")), like),
                        cb.like(cb.lower(root.get("clientIp")), like)));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventTime"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventTime"), filter.to()));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

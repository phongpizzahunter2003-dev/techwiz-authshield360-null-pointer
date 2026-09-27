package com.authshield360.audit.dto;

import java.time.Instant;

/** Row for the audit log viewer / export (UC-11). PII is masked by the export service. */
public record AuditLogResponse(
        Long id,
        Instant eventTime,
        String eventId,
        String eventAction,
        String status,
        String authFactor,
        String authMode,
        String userIdentifier,
        String role,
        String clientIp,
        String sessionId,
        String failureReason,
        String correlationId,
        String detail
) {
}

package com.authshield360.audit;

import java.time.Instant;

/** Filter criteria for the audit viewer/export (UC-11). */
public record AuditFilter(
        String role,
        String action,
        String status,
        String mode,
        String query,
        Instant from,
        Instant to
) {
    public static AuditFilter empty() {
        return new AuditFilter(null, null, null, null, null, null, null);
    }
}

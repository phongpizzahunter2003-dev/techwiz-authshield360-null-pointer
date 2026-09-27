package com.authshield360.system.dto;

import java.time.Instant;
import java.util.List;

/**
 * Diagnostic snapshot proving where data is actually being written.
 * Never exposes credentials (BR-10).
 */
public record DbStatusResponse(
        String databaseProduct,
        String databaseVersion,
        String jdbcUrl,
        boolean persistent,
        String activeProfiles,
        List<DbTableStatus> tables,
        long totalRows,
        Instant checkedAt
) {
}

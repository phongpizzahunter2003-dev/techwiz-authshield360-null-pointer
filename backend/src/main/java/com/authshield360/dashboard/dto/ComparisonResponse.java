package com.authshield360.dashboard.dto;

import java.time.Instant;
import java.util.List;

public record ComparisonResponse(
        List<ModeComparison> modes,
        Instant generatedAt,
        String note
) {
}

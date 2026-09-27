package com.authshield360.analytics.dto;

import java.util.List;

/** Role-specific analytics payload consumed by the dashboard charts. */
public record AnalyticsResponse(String role, String greeting, List<ChartSeries> charts) {
}

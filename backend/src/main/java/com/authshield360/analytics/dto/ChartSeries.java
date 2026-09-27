package com.authshield360.analytics.dto;

import java.util.List;

/**
 * A chart on a role dashboard. {@code type} is one of BAR | PIE | LINE.
 * Every slice carries a drill-down route so the whole chart is navigable.
 */
public record ChartSeries(
        String key,
        String title,
        String type,
        String valueLabel,
        String description,
        List<ChartSlice> slices
) {
    public static final String BAR = "BAR";
    public static final String PIE = "PIE";
    public static final String LINE = "LINE";
}

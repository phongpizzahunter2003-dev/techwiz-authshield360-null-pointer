package com.authshield360.analytics.dto;

/**
 * One clickable segment/bar/point of a chart.
 *
 * @param key    machine key (e.g. ON_TIME, S1, ADMIN)
 * @param label  display label
 * @param value  numeric value
 * @param tone   colour token hint for the UI (brand|accent|sun|coral|sky)
 * @param drill  front-end route to open when the slice is clicked (may include a query string)
 */
public record ChartSlice(String key, String label, long value, String tone, String drill) {
}

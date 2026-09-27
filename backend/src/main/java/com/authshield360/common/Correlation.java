package com.authshield360.common;

import org.slf4j.MDC;

/** Correlation id propagation (be-rules.md §9). */
public final class Correlation {
    public static final String MDC_KEY = "correlationId";
    private Correlation() { }

    public static String current() {
        String v = MDC.get(MDC_KEY);
        return v == null ? null : v;
    }
}

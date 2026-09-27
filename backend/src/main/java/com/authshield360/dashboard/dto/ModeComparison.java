package com.authshield360.dashboard.dto;

import com.authshield360.auth.AuthMode;

/** One row of the S1/S2/S3 comparison (UC-15). */
public record ModeComparison(
        AuthMode mode,
        String name,
        String security,
        String usability,
        String riskReduced,
        long loginSuccesses,
        long loginFailures,
        long otpFailures
) {
}

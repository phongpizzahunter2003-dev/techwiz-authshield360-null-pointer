package com.authshield360.auth.dto;

import com.authshield360.auth.OtpFactor;

import java.time.Instant;

/**
 * Result of a sign-in step. {@code status} is either {@code AUTHENTICATED} (an access token is
 * present) or {@code OTP_REQUIRED} (a challenge token and factor are present).
 */
public record LoginResponse(
        String status,
        String message,
        String token,
        Instant tokenExpiresAt,
        String challengeToken,
        OtpFactor factor,
        Long otpExpiresInSeconds,
        String deliveryCode,
        String deliveryChannel,
        boolean totpBased,
        UserSummary user
) {
    public static final String AUTHENTICATED = "AUTHENTICATED";
    public static final String OTP_REQUIRED = "OTP_REQUIRED";

    public static LoginResponse authenticated(String token, Instant expiresAt, UserSummary user) {
        return new LoginResponse(AUTHENTICATED, "Signed in successfully. Redirecting...",
                token, expiresAt, null, null, null, null, null, false, user);
    }

    public static LoginResponse otpRequired(String challengeToken, OtpFactor factor, Long expiresInSeconds,
                                            String deliveryCode, String deliveryChannel, boolean totpBased) {
        String message = factor == OtpFactor.EMAIL_OTP
                ? "Mobile OTP verified. A verification code has been sent to your email."
                : "A verification code has been sent to your device. Please check it.";
        return new LoginResponse(OTP_REQUIRED, message, null, null, challengeToken, factor,
                expiresInSeconds, deliveryCode, deliveryChannel, totpBased, null);
    }
}

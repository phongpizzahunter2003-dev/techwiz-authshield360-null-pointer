package com.authshield360.auth.dto;

import com.authshield360.auth.OtpFactor;

import java.time.Instant;

/**
 * Login step result. {@code status} is either AUTHENTICATED (a token is present) or OTP_REQUIRED
 * (a challenge token + factor is present).
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
        return new LoginResponse(AUTHENTICATED, "Đăng nhập thành công. Đang chuyển hướng...",
                token, expiresAt, null, null, null, null, null, false, user);
    }

    public static LoginResponse otpRequired(String challengeToken, OtpFactor factor, Long expiresInSeconds,
                                            String deliveryCode, String deliveryChannel, boolean totpBased) {
        String message = factor == OtpFactor.EMAIL_OTP
                ? "Xác thực Mobile OTP thành công. Một mã xác minh đã được gửi đến email của bạn."
                : "Mã OTP đã được gửi đến thiết bị của bạn. Vui lòng kiểm tra.";
        return new LoginResponse(OTP_REQUIRED, message, null, null, challengeToken, factor,
                expiresInSeconds, deliveryCode, deliveryChannel, totpBased, null);
    }
}

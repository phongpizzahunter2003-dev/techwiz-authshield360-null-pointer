package com.authshield360.common;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes with the canonical user-facing message.
 * The API is English-only; the SPA renders {@code code} for field-level feedback.
 */
public enum ErrorCode {

    // --- generic ---
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid data. Please check your input."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
    CONFLICT(HttpStatus.CONFLICT, "The resource already exists."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A system error occurred. Please try again later."),

    // --- auth (UC-01..UC-04) ---
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Incorrect username or password. Please try again."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "Your account has been temporarily locked after too many failed attempts."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "This account is disabled. Please contact an administrator."),
    OTP_REQUIRED(HttpStatus.UNAUTHORIZED, "OTP verification is required."),
    OTP_INVALID(HttpStatus.UNAUTHORIZED, "The verification code is incorrect. Please check and try again."),
    OTP_EXPIRED(HttpStatus.UNAUTHORIZED, "The verification code has expired. Please request a new one."),
    OTP_RESENT(HttpStatus.OK, "A new verification code has been sent. Please check your device."),
    RESEND_THROTTLED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait before requesting another code."),
    RESEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "You have requested too many codes. Please start the sign-in process again."),
    CAPTCHA_REQUIRED(HttpStatus.BAD_REQUEST, "Please complete the challenge to confirm you are not a robot."),
    INVALID_CHALLENGE(HttpStatus.UNAUTHORIZED, "The verification session is invalid or has expired. Please sign in again."),
    MFA_NOT_ENROLLED(HttpStatus.CONFLICT, "This account has not enrolled a second factor yet."),
    MFA_ALREADY_ENROLLED(HttpStatus.CONFLICT, "This account has already enrolled a second factor."),
    INVALID_MFA_CODE(HttpStatus.BAD_REQUEST, "The confirmation code is incorrect. Please try again."),

    // --- session (UC-06) ---
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Your session is invalid. Please sign in again."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again."),

    // --- authorization (UC-05) ---
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to access this resource. This attempt has been logged."),

    // --- users (UC-07) ---
    USERNAME_EXISTS(HttpStatus.CONFLICT, "That username or email is already taken. Please choose another."),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "That username or email is already taken. Please choose another."),

    // --- config (UC-08) ---
    SMTP_CONNECTION_FAILED(HttpStatus.BAD_REQUEST, "Could not connect to the SMTP server. Please check the configuration."),
    EMAIL_OTP_UNSUPPORTED(HttpStatus.BAD_REQUEST, "Email OTP is not supported by the current configuration."),

    // --- audit (UC-11) ---
    EXPORT_LIMIT_EXCEEDED(HttpStatus.CONTENT_TOO_LARGE, "The result set exceeds the export limit for a single request."),

    // --- assignments / submissions (UC-A1..A4) ---
    SUBMISSION_LOCKED(HttpStatus.CONFLICT, "This assignment is closed. You cannot update your submission."),
    RESUBMISSION_NOT_ALLOWED(HttpStatus.CONFLICT, "Resubmission is not allowed for this assignment."),
    MAX_ATTEMPTS_REACHED(HttpStatus.CONFLICT, "You have reached the maximum number of submissions."),
    LATE_NOT_ALLOWED(HttpStatus.CONFLICT, "The deadline has passed. Late submissions are not accepted."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "Invalid file. Please check the format and size."),
    NOT_ENROLLED(HttpStatus.FORBIDDEN, "You are not enrolled in this class.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}

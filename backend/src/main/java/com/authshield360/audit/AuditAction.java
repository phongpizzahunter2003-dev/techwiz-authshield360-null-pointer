package com.authshield360.audit;

/** Canonical audit event actions (ba-rules.md §5). */
public final class AuditAction {
    private AuditAction() { }

    public static final String LOGIN_ATTEMPT = "LOGIN_ATTEMPT";
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOCKOUT_TRIGGERED = "LOCKOUT_TRIGGERED";
    public static final String LOCKOUT_RELEASED = "LOCKOUT_RELEASED";
    public static final String OTP_SENT = "OTP_SENT";
    public static final String OTP_VERIFY_SUCCESS = "OTP_VERIFY_SUCCESS";
    public static final String OTP_VERIFY_FAIL = "OTP_VERIFY_FAIL";
    public static final String OTP_EXPIRED = "OTP_EXPIRED";
    public static final String RESEND_OTP_REQUEST = "RESEND_OTP_REQUEST";
    public static final String RESEND_OTP_SUCCESS = "RESEND_OTP_SUCCESS";
    public static final String RESEND_OTP_LIMIT_EXCEEDED = "RESEND_OTP_LIMIT_EXCEEDED";
    public static final String RESEND_OTP_THROTTLED = "RESEND_OTP_THROTTLED";
    public static final String EMAIL_OTP_SENT = "EMAIL_OTP_SENT";
    public static final String EMAIL_OTP_FAILED = "EMAIL_OTP_FAILED";
    public static final String EMAIL_OTP_EXPIRED = "EMAIL_OTP_EXPIRED";
    public static final String MFA_ENROLL_SUCCESS = "MFA_ENROLL_SUCCESS";
    public static final String MFA_ENROLL_FAIL = "MFA_ENROLL_FAIL";
    public static final String LOGOUT = "LOGOUT";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
    public static final String SESSION_REPLAY_ATTEMPT = "SESSION_REPLAY_ATTEMPT";
    public static final String PRIVILEGE_VIOLATION = "PRIVILEGE_VIOLATION";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String ROLE_ASSIGN = "ROLE_ASSIGN";
    public static final String CONFIG_CHANGE = "CONFIG_CHANGE";
    public static final String LOG_VIEW = "LOG_VIEW";
    public static final String LOG_EXPORT = "LOG_EXPORT";
    public static final String ACCOUNT_RECOVERY_REQUEST = "ACCOUNT_RECOVERY_REQUEST";
    public static final String MFA_RESET = "MFA_RESET";
    public static final String STEP_UP_AUTH_SUCCESS = "STEP_UP_AUTH_SUCCESS";
    public static final String STEP_UP_AUTH_FAIL = "STEP_UP_AUTH_FAIL";
    public static final String ASSIGNMENT_CREATE = "ASSIGNMENT_CREATE";
    public static final String ASSIGNMENT_UPDATE = "ASSIGNMENT_UPDATE";
    public static final String ASSIGNMENT_CLOSE = "ASSIGNMENT_CLOSE";
    public static final String SUBMISSION_CREATE = "SUBMISSION_CREATE";
    public static final String SUBMISSION_UPDATE = "SUBMISSION_UPDATE";
    public static final String SUBMISSION_BLOCKED = "SUBMISSION_BLOCKED";
    public static final String SUBMISSION_GRADE = "SUBMISSION_GRADE";
    public static final String EXAM_RESULT_UPSERT = "EXAM_RESULT_UPSERT";
}

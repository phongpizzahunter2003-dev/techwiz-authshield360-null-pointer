package com.authshield360.notification;

/** Notification event types (kept as constants so they are easy to filter/translate). */
public final class NotificationType {
    private NotificationType() { }

    public static final String ASSIGNMENT_PUBLISHED = "ASSIGNMENT_PUBLISHED";
    public static final String ASSIGNMENT_UPDATED = "ASSIGNMENT_UPDATED";
    public static final String ASSIGNMENT_CLOSED = "ASSIGNMENT_CLOSED";
    public static final String SUBMISSION_RECEIVED = "SUBMISSION_RECEIVED";
    public static final String SUBMISSION_LATE = "SUBMISSION_LATE";
    public static final String SUBMISSION_GRADED = "SUBMISSION_GRADED";
    public static final String CLASSROOM_ENROLLED = "CLASSROOM_ENROLLED";
    public static final String ACCOUNT_READY = "ACCOUNT_READY";
}

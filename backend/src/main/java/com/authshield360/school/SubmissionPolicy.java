package com.authshield360.school;

/**
 * Pure decision logic for submission handling (UC-A1..UC-A4).
 * Kept side-effect free so it is trivially unit-testable (qa-rules.md §8).
 */
public final class SubmissionPolicy {

    private SubmissionPolicy() { }

    public enum Decision { ON_TIME, LATE, BLOCKED }

    public record Evaluation(Decision decision, SubmissionStatus status, String lockReason, String message) {

        public boolean blocked() { return decision == Decision.BLOCKED; }

        static Evaluation onTime() {
            return new Evaluation(Decision.ON_TIME, SubmissionStatus.ON_TIME, null, null);
        }

        static Evaluation late() {
            return new Evaluation(Decision.LATE, SubmissionStatus.LATE, null, null);
        }

        static Evaluation blocked(String reason, String message) {
            return new Evaluation(Decision.BLOCKED, null, reason, message);
        }
    }

    /**
     * @param assignment        the assignment policy
     * @param existingAttempts  number of attempts already recorded for this student
     * @param graded            whether an attempt has already been graded
     * @param now               current time
     */
    public static Evaluation evaluate(Assignment assignment, int existingAttempts, boolean graded,
                                      java.time.Instant now) {
        // A closed assignment locks everything, regardless of resubmission settings (UC-A3).
        if (assignment.getStatus() == AssignmentStatus.CLOSED) {
            return Evaluation.blocked("SUBMISSION_LOCKED", "This assignment is closed. You cannot update your submission.");
        }

        if (existingAttempts > 0) {
            if (!assignment.isAllowResubmission()) {
                return Evaluation.blocked("RESUBMISSION_NOT_ALLOWED", "Resubmission is not allowed for this assignment.");
            }
            if (graded) {
                return Evaluation.blocked("RESUBMISSION_NOT_ALLOWED", "This submission has been graded and can no longer be resubmitted.");
            }
            if (existingAttempts >= assignment.getMaxAttempts()) {
                return Evaluation.blocked("MAX_ATTEMPTS_REACHED", "You have reached the maximum number of submissions.");
            }
        }

        boolean isLate = now.isAfter(assignment.getDueAt());
        if (isLate) {
            if (!assignment.isAllowLate()) {
                return Evaluation.blocked("LATE_NOT_ALLOWED", "The deadline has passed. Late submissions are not accepted.");
            }
            if (assignment.getLateCutoffAt() != null && now.isAfter(assignment.getLateCutoffAt())) {
                return Evaluation.blocked("LATE_NOT_ALLOWED", "The late submission window has closed.");
            }
            return Evaluation.late();
        }
        return Evaluation.onTime();
    }
}

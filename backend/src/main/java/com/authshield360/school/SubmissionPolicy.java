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
            return Evaluation.blocked("SUBMISSION_LOCKED", "Bài tập đã đóng. Bạn không thể cập nhật bài nộp.");
        }

        if (existingAttempts > 0) {
            if (!assignment.isAllowResubmission()) {
                return Evaluation.blocked("RESUBMISSION_NOT_ALLOWED", "Bài tập này không cho phép nộp lại.");
            }
            if (graded) {
                return Evaluation.blocked("RESUBMISSION_NOT_ALLOWED", "Bài đã được chấm điểm, không thể nộp lại.");
            }
            if (existingAttempts >= assignment.getMaxAttempts()) {
                return Evaluation.blocked("MAX_ATTEMPTS_REACHED", "Bạn đã đạt số lần nộp tối đa.");
            }
        }

        boolean isLate = now.isAfter(assignment.getDueAt());
        if (isLate) {
            if (!assignment.isAllowLate()) {
                return Evaluation.blocked("LATE_NOT_ALLOWED", "Đã quá hạn nộp bài. Hệ thống không nhận bài muộn.");
            }
            if (assignment.getLateCutoffAt() != null && now.isAfter(assignment.getLateCutoffAt())) {
                return Evaluation.blocked("LATE_NOT_ALLOWED", "Đã quá thời hạn nộp muộn cho phép.");
            }
            return Evaluation.late();
        }
        return Evaluation.onTime();
    }
}

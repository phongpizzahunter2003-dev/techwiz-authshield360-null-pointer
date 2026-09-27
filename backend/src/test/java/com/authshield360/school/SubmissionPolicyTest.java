package com.authshield360.school;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Truth-table coverage for UC-A1..UC-A4 (see docs/use-cases.md).
 * Pure logic, no Spring context required.
 */
class SubmissionPolicyTest {

    private static final Instant NOW = Instant.parse("2026-09-27T10:00:00Z");

    private Assignment assignment(Instant dueAt, boolean allowLate, Instant cutoff,
                                  boolean allowResubmission, int maxAttempts) {
        Assignment a = new Assignment();
        a.setDueAt(dueAt);
        a.setAllowLate(allowLate);
        a.setLateCutoffAt(cutoff);
        a.setAllowResubmission(allowResubmission);
        a.setMaxAttempts(maxAttempts);
        a.setStatus(AssignmentStatus.PUBLISHED);
        return a;
    }

    @Test
    void onTimeFirstSubmission() {
        Assignment a = assignment(NOW.plusSeconds(3600), true, null, true, 3);
        var result = SubmissionPolicy.evaluate(a, 0, false, NOW);
        assertThat(result.decision()).isEqualTo(SubmissionPolicy.Decision.ON_TIME);
        assertThat(result.status()).isEqualTo(SubmissionStatus.ON_TIME);
    }

    @Test
    void lateSubmissionAllowedWithinCutoff() {
        Assignment a = assignment(NOW.minusSeconds(3600), true, NOW.plusSeconds(3600), true, 3);
        var result = SubmissionPolicy.evaluate(a, 0, false, NOW);
        assertThat(result.decision()).isEqualTo(SubmissionPolicy.Decision.LATE);
        assertThat(result.status()).isEqualTo(SubmissionStatus.LATE);
    }

    @Test
    void lateSubmissionRejectedWhenLateDisallowed() {
        Assignment a = assignment(NOW.minusSeconds(3600), false, null, true, 3);
        var result = SubmissionPolicy.evaluate(a, 0, false, NOW);
        assertThat(result.blocked()).isTrue();
        assertThat(result.lockReason()).isEqualTo("LATE_NOT_ALLOWED");
    }

    @Test
    void lateSubmissionRejectedAfterCutoff() {
        Assignment a = assignment(NOW.minusSeconds(7200), true, NOW.minusSeconds(60), true, 3);
        var result = SubmissionPolicy.evaluate(a, 0, false, NOW);
        assertThat(result.blocked()).isTrue();
        assertThat(result.lockReason()).isEqualTo("LATE_NOT_ALLOWED");
    }

    @Test
    void closedAssignmentBlocksEverything() {
        Assignment a = assignment(NOW.plusSeconds(3600), true, null, true, 3);
        a.setStatus(AssignmentStatus.CLOSED);
        var result = SubmissionPolicy.evaluate(a, 2, false, NOW);
        assertThat(result.blocked()).isTrue();
        assertThat(result.lockReason()).isEqualTo("SUBMISSION_LOCKED");
    }

    @Test
    void resubmissionAllowedWhileOpen() {
        Assignment a = assignment(NOW.plusSeconds(3600), true, null, true, 3);
        var result = SubmissionPolicy.evaluate(a, 1, false, NOW);
        assertThat(result.decision()).isEqualTo(SubmissionPolicy.Decision.ON_TIME);
    }

    @Test
    void resubmissionRejectedWhenDisabled() {
        Assignment a = assignment(NOW.plusSeconds(3600), true, null, false, 3);
        var result = SubmissionPolicy.evaluate(a, 1, false, NOW);
        assertThat(result.blocked()).isTrue();
        assertThat(result.lockReason()).isEqualTo("RESUBMISSION_NOT_ALLOWED");
    }

    @Test
    void resubmissionRejectedWhenAlreadyGraded() {
        Assignment a = assignment(NOW.plusSeconds(3600), true, null, true, 3);
        var result = SubmissionPolicy.evaluate(a, 1, true, NOW);
        assertThat(result.blocked()).isTrue();
        assertThat(result.lockReason()).isEqualTo("RESUBMISSION_NOT_ALLOWED");
    }

    @Test
    void resubmissionRejectedAtMaxAttempts() {
        Assignment a = assignment(NOW.plusSeconds(3600), true, null, true, 2);
        var result = SubmissionPolicy.evaluate(a, 2, false, NOW);
        assertThat(result.blocked()).isTrue();
        assertThat(result.lockReason()).isEqualTo("MAX_ATTEMPTS_REACHED");
    }
}

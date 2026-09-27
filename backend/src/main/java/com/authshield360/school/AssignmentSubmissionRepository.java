package com.authshield360.school;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    List<AssignmentSubmission> findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(Long assignmentId, Long studentId);

    Optional<AssignmentSubmission> findTopByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(Long assignmentId, Long studentId);

    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);

    List<AssignmentSubmission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    long countByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}

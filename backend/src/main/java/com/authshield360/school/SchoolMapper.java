package com.authshield360.school;

import com.authshield360.school.dto.*;
import com.authshield360.security.CurrentUser;
import com.authshield360.user.RoleType;
import com.authshield360.user.User;
import com.authshield360.user.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Builds school DTOs, including the UC-A1..UC-A4 submission state for students. */
@Component
public class SchoolMapper {

    private final ClassroomRepository classrooms;
    private final AssignmentSubmissionRepository submissions;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;

    public SchoolMapper(ClassroomRepository classrooms, AssignmentSubmissionRepository submissions,
                        EnrollmentRepository enrollments, UserRepository users) {
        this.classrooms = classrooms;
        this.submissions = submissions;
        this.enrollments = enrollments;
        this.users = users;
    }

    public String nameOf(Long userId) {
        if (userId == null) return null;
        return users.findById(userId).map(User::getFullName).filter(n -> n != null && !n.isBlank())
                .orElseGet(() -> users.findById(userId).map(User::getUsername).orElse(null));
    }

    public ClassroomResponse toClassroom(Classroom c) {
        return new ClassroomResponse(c.getId(), c.getCode(), c.getName(), c.getDescription(),
                c.getTeacherId(), nameOf(c.getTeacherId()), enrollments.countByClassroomId(c.getId()));
    }

    public SubmissionResponse toSubmission(AssignmentSubmission s, Assignment assignment, String studentName,
                                           boolean current) {
        return new SubmissionResponse(
                s.getId(), s.getAssignmentId(), assignment == null ? null : assignment.getTitle(),
                s.getStudentId(), studentName, s.getAttemptNumber(), s.getOriginalName(), s.getContentType(),
                s.getSizeBytes(), s.getSubmittedAt(), s.getSubmissionStatus(),
                s.getSubmissionStatus() == SubmissionStatus.LATE, s.getScore(), s.getFeedback(),
                s.getGradedAt(), current);
    }

    public AssignmentResponse toAssignment(Assignment a, CurrentUser viewer, boolean withSubmissions) {
        String className = classrooms.findById(a.getClassroomId()).map(Classroom::getName).orElse(null);
        Integer attempts = null;
        Boolean canSubmit = null;
        String lockReason = null;
        SubmissionResponse latest = null;
        List<SubmissionResponse> history = null;

        if (viewer != null && RoleType.STUDENT.name().equals(viewer.role())) {
            List<AssignmentSubmission> own =
                    submissions.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(a.getId(), viewer.userId());
            attempts = own.size();
            boolean graded = own.stream().anyMatch(s -> s.getScore() != null);
            SubmissionPolicy.Evaluation eval = SubmissionPolicy.evaluate(a, own.size(), graded, Instant.now());
            canSubmit = !eval.blocked();
            lockReason = eval.lockReason();
            if (!own.isEmpty()) {
                latest = toSubmission(own.get(0), a, viewer.username(), true);
            }
            if (withSubmissions) {
                final Assignment assignment = a;
                history = own.stream().map(s -> toSubmission(s, assignment, viewer.username(),
                        !own.isEmpty() && s.getId().equals(own.get(0).getId()))).toList();
            }
        }

        return new AssignmentResponse(
                a.getId(), a.getTitle(), a.getDescription(), a.getClassroomId(), className,
                a.getCreatedBy(), nameOf(a.getCreatedBy()), a.getDueAt(), a.isAllowLate(), a.getLateCutoffAt(),
                a.getLatePenaltyPct(), a.isAllowResubmission(), a.getMaxAttempts(), a.getMaxScore(), a.getStatus(),
                a.getCreatedAt(), attempts, canSubmit, lockReason, latest, history);
    }

    public boolean isEnrolled(Long studentId, Long classroomId) {
        return enrollments.existsByClassroomIdAndStudentId(classroomId, studentId);
    }
}

package com.authshield360.school;

import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditEvent;
import com.authshield360.audit.AuditService;
import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.school.dto.GradeRequest;
import com.authshield360.school.dto.SubmissionResponse;
import com.authshield360.security.CurrentUser;
import com.authshield360.user.RoleType;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * Handles student submissions and teacher grading, implementing UC-A1 (on time),
 * UC-A2 (late), UC-A3 (locked / no update) and UC-A4 (update submitted file / versioning).
 */
@Service
public class SubmissionService {

    private final AssignmentRepository assignments;
    private final AssignmentSubmissionRepository submissions;
    private final EnrollmentRepository enrollments;
    private final AssignmentService assignmentService;
    private final FileStorageService fileStorage;
    private final SchoolMapper mapper;
    private final AuditService audit;

    public SubmissionService(AssignmentRepository assignments, AssignmentSubmissionRepository submissions,
                             EnrollmentRepository enrollments, AssignmentService assignmentService,
                             FileStorageService fileStorage, SchoolMapper mapper, AuditService audit) {
        this.assignments = assignments;
        this.submissions = submissions;
        this.enrollments = enrollments;
        this.assignmentService = assignmentService;
        this.fileStorage = fileStorage;
        this.mapper = mapper;
        this.audit = audit;
    }

    public record SubmissionDownload(Resource resource, String filename, String contentType) { }

    /** UC-A1 / UC-A2 / UC-A3 / UC-A4 — create a new attempt or reject a locked update. */
    @Transactional
    public SubmissionResponse submit(Long assignmentId, MultipartFile file, CurrentUser student) {
        Assignment assignment = assignmentService.findOrThrow(assignmentId);

        if (!enrollments.existsByClassroomIdAndStudentId(assignment.getClassroomId(), student.userId())) {
            audit.record(AuditEvent.action(AuditAction.PRIVILEGE_VIOLATION).failure("ACCESS_DENIED")
                    .user(student.username()).role(student.role())
                    .detail("assignmentId", assignmentId).detail("reason", "NOT_ENROLLED"));
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        List<AssignmentSubmission> existing =
                submissions.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignmentId, student.userId());
        boolean graded = existing.stream().anyMatch(s -> s.getScore() != null);

        SubmissionPolicy.Evaluation evaluation =
                SubmissionPolicy.evaluate(assignment, existing.size(), graded, Instant.now());
        if (evaluation.blocked()) {
            audit.record(AuditEvent.action(AuditAction.SUBMISSION_BLOCKED).failure(evaluation.lockReason())
                    .user(student.username()).role(student.role())
                    .detail("assignmentId", assignmentId).detail("attempt", existing.size()));
            throw new BusinessException(mapLockReason(evaluation.lockReason()), evaluation.message());
        }

        FileStorageService.StoredFile stored = fileStorage.store(file, assignmentId, student.userId());

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(student.userId());
        submission.setAttemptNumber(existing.size() + 1);
        submission.setOriginalName(stored.originalName());
        submission.setStoredName(stored.storedName());
        submission.setContentType(stored.contentType());
        submission.setSizeBytes(stored.size());
        submission.setSubmittedAt(Instant.now());
        submission.setSubmissionStatus(evaluation.status());
        AssignmentSubmission saved = submissions.save(submission);

        boolean isUpdate = existing.size() > 0;
        audit.record(AuditEvent.action(isUpdate ? AuditAction.SUBMISSION_UPDATE : AuditAction.SUBMISSION_CREATE)
                .success().user(student.username()).role(student.role())
                .detail("assignmentId", assignmentId).detail("attempt", saved.getAttemptNumber())
                .detail("status", saved.getSubmissionStatus().name()));

        return mapper.toSubmission(saved, assignment, student.username(), true);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> history(Long assignmentId, CurrentUser student) {
        Assignment assignment = assignmentService.findOrThrow(assignmentId);
        List<AssignmentSubmission> own =
                submissions.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignmentId, student.userId());
        String name = mapper.nameOf(student.userId());
        return own.stream()
                .map(s -> mapper.toSubmission(s, assignment, name, !own.isEmpty() && s.getId().equals(own.get(0).getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listForStudent(CurrentUser student) {
        List<AssignmentSubmission> all = submissions.findByStudentIdOrderBySubmittedAtDesc(student.userId());
        String name = mapper.nameOf(student.userId());
        return all.stream().map(s -> {
            Assignment a = assignments.findById(s.getAssignmentId()).orElse(null);
            return mapper.toSubmission(s, a, name, true);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listForAssignment(Long assignmentId, CurrentUser viewer) {
        Assignment assignment = assignmentService.findOrThrow(assignmentId);
        assignmentService.assertCanManage(assignment, viewer);
        return submissions.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId).stream()
                .map(s -> mapper.toSubmission(s, assignment, mapper.nameOf(s.getStudentId()), false))
                .toList();
    }

    @Transactional
    public SubmissionResponse grade(Long submissionId, GradeRequest req, CurrentUser viewer) {
        AssignmentSubmission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Assignment assignment = assignmentService.findOrThrow(submission.getAssignmentId());
        assignmentService.assertCanManage(assignment, viewer);
        if (req.score() > assignment.getMaxScore()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Điểm không được vượt quá điểm tối đa (" + assignment.getMaxScore() + ").");
        }
        submission.setScore(req.score());
        submission.setFeedback(req.feedback());
        submission.setGradedBy(viewer.userId());
        submission.setGradedAt(Instant.now());
        AssignmentSubmission saved = submissions.save(submission);
        audit.record(AuditEvent.action(AuditAction.SUBMISSION_GRADE).success()
                .user(viewer.username()).role(viewer.role())
                .detail("submissionId", saved.getId()).detail("score", req.score()));
        return mapper.toSubmission(saved, assignment, mapper.nameOf(saved.getStudentId()), true);
    }

    @Transactional(readOnly = true)
    public SubmissionDownload download(Long submissionId, CurrentUser viewer) {
        AssignmentSubmission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        boolean isOwner = RoleType.STUDENT.name().equals(viewer.role())
                && submission.getStudentId().equals(viewer.userId());
        if (!isOwner) {
            Assignment assignment = assignmentService.findOrThrow(submission.getAssignmentId());
            assignmentService.assertCanManage(assignment, viewer);
        }
        return new SubmissionDownload(fileStorage.load(submission.getStoredName()),
                submission.getOriginalName(), submission.getContentType());
    }

    private ErrorCode mapLockReason(String reason) {
        return switch (reason) {
            case "SUBMISSION_LOCKED" -> ErrorCode.SUBMISSION_LOCKED;
            case "MAX_ATTEMPTS_REACHED" -> ErrorCode.MAX_ATTEMPTS_REACHED;
            case "LATE_NOT_ALLOWED" -> ErrorCode.LATE_NOT_ALLOWED;
            default -> ErrorCode.RESUBMISSION_NOT_ALLOWED;
        };
    }
}

package com.authshield360.school;

import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditEvent;
import com.authshield360.audit.AuditService;
import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.notification.NotificationService;
import com.authshield360.school.dto.AssignmentResponse;
import com.authshield360.school.dto.CreateAssignmentRequest;
import com.authshield360.school.dto.UpdateAssignmentRequest;
import com.authshield360.security.CurrentUser;
import com.authshield360.user.RoleType;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Assignment lifecycle: create/edit/publish/close (teacher) and role-aware listing (student). */
@Service
public class AssignmentService {

    private final AssignmentRepository assignments;
    private final EnrollmentRepository enrollments;
    private final ClassroomService classroomService;
    private final SchoolMapper mapper;
    private final AuditService audit;
    private final NotificationService notifications;

    public AssignmentService(AssignmentRepository assignments, EnrollmentRepository enrollments,
                             ClassroomService classroomService, SchoolMapper mapper, AuditService audit,
                             NotificationService notifications) {
        this.assignments = assignments;
        this.enrollments = enrollments;
        this.classroomService = classroomService;
        this.mapper = mapper;
        this.audit = audit;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listFor(CurrentUser viewer) {
        List<Assignment> result;
        switch (RoleType.valueOf(viewer.role())) {
            case STUDENT -> {
                List<Long> ids = enrollments.findByStudentId(viewer.userId()).stream()
                        .map(Enrollment::getClassroomId).toList();
                result = ids.isEmpty() ? List.of() : assignments.findByClassroomIdInOrderByDueAtDesc(ids);
            }
            case TEACHER -> result = assignments.findByCreatedByOrderByCreatedAtDesc(viewer.userId());
            case ADMIN -> result = assignments.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> result = List.of();
        }
        return result.stream().map(a -> mapper.toAssignment(a, viewer, false)).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getFor(Long id, CurrentUser viewer) {
        Assignment assignment = findOrThrow(id);
        assertCanView(assignment, viewer);
        return mapper.toAssignment(assignment, viewer, true);
    }

    @Transactional(readOnly = true)
    public Assignment findOrThrow(Long id) {
        return assignments.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** A student may only see assignments of classes they belong to. */
    @Transactional(readOnly = true)
    public void assertCanView(Assignment assignment, CurrentUser viewer) {
        if (RoleType.STUDENT.name().equals(viewer.role())
                && !enrollments.existsByClassroomIdAndStudentId(assignment.getClassroomId(), viewer.userId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /** A teacher may only manage assignments of classes they own; admin always may. */
    @Transactional(readOnly = true)
    public void assertCanManage(Assignment assignment, CurrentUser viewer) {
        if (RoleType.ADMIN.name().equals(viewer.role())) {
            return;
        }
        if (RoleType.TEACHER.name().equals(viewer.role())) {
            boolean creator = assignment.getCreatedBy().equals(viewer.userId());
            Long owner = classroomService.getOrThrow(assignment.getClassroomId()).getTeacherId();
            if (creator || (owner != null && owner.equals(viewer.userId()))) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public AssignmentResponse create(CreateAssignmentRequest req, CurrentUser viewer) {
        classroomService.assertCanManage(req.classroomId(), viewer);
        Assignment assignment = new Assignment();
        assignment.setTitle(req.title().trim());
        assignment.setDescription(req.description());
        assignment.setClassroomId(req.classroomId());
        assignment.setCreatedBy(viewer.userId());
        assignment.setDueAt(req.dueAt());
        assignment.setAllowLate(req.allowLate() == null || req.allowLate());
        assignment.setLateCutoffAt(req.lateCutoffAt());
        assignment.setLatePenaltyPct(req.latePenaltyPct() == null ? 10 : req.latePenaltyPct());
        assignment.setAllowResubmission(req.allowResubmission() == null || req.allowResubmission());
        assignment.setMaxAttempts(req.maxAttempts() == null ? 3 : req.maxAttempts());
        assignment.setMaxScore(req.maxScore() == null ? 100 : req.maxScore());
        assignment.setStatus(req.status() == null ? AssignmentStatus.PUBLISHED : req.status());
        Assignment saved = assignments.save(assignment);
        audit.record(AuditEvent.action(AuditAction.ASSIGNMENT_CREATE).success()
                .user(viewer.username()).role(viewer.role())
                .detail("assignmentId", saved.getId()).detail("classroomId", saved.getClassroomId()));
        if (saved.getStatus() == AssignmentStatus.PUBLISHED) {
            notifications.assignmentPublished(saved);
        }
        return mapper.toAssignment(saved, viewer, false);
    }

    @Transactional
    public AssignmentResponse update(Long id, UpdateAssignmentRequest req, CurrentUser viewer) {
        Assignment assignment = findOrThrow(id);
        assertCanManage(assignment, viewer);
        if (req.title() != null) assignment.setTitle(req.title().trim());
        if (req.description() != null) assignment.setDescription(req.description());
        if (req.dueAt() != null) assignment.setDueAt(req.dueAt());
        if (req.allowLate() != null) assignment.setAllowLate(req.allowLate());
        if (req.lateCutoffAt() != null) assignment.setLateCutoffAt(req.lateCutoffAt());
        if (req.latePenaltyPct() != null) assignment.setLatePenaltyPct(req.latePenaltyPct());
        if (req.allowResubmission() != null) assignment.setAllowResubmission(req.allowResubmission());
        if (req.maxAttempts() != null) assignment.setMaxAttempts(req.maxAttempts());
        if (req.maxScore() != null) assignment.setMaxScore(req.maxScore());
        if (req.status() != null) assignment.setStatus(req.status());
        Assignment saved = assignments.save(assignment);
        audit.record(AuditEvent.action(AuditAction.ASSIGNMENT_UPDATE).success()
                .user(viewer.username()).role(viewer.role())
                .detail("assignmentId", saved.getId()).detail("status", saved.getStatus().name()));
        if (saved.getStatus() == AssignmentStatus.PUBLISHED) {
            notifications.assignmentUpdated(saved);
        }
        return mapper.toAssignment(saved, viewer, false);
    }

    @Transactional
    public AssignmentResponse close(Long id, CurrentUser viewer) {
        Assignment assignment = findOrThrow(id);
        assertCanManage(assignment, viewer);
        assignment.setStatus(AssignmentStatus.CLOSED);
        Assignment saved = assignments.save(assignment);
        audit.record(AuditEvent.action(AuditAction.ASSIGNMENT_CLOSE).success()
                .user(viewer.username()).role(viewer.role()).detail("assignmentId", saved.getId()));
        notifications.assignmentClosed(saved);
        return mapper.toAssignment(saved, viewer, false);
    }
}

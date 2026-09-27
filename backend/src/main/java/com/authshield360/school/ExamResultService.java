package com.authshield360.school;

import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditEvent;
import com.authshield360.audit.AuditService;
import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.school.dto.ExamResultResponse;
import com.authshield360.school.dto.UpsertExamResultRequest;
import com.authshield360.security.CurrentUser;
import com.authshield360.user.RoleType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Exam results entry/viewing (RBAC: student own, teacher own classes, admin all). */
@Service
public class ExamResultService {

    private final ExamResultRepository results;
    private final EnrollmentRepository enrollments;
    private final ClassroomRepository classrooms;
    private final SchoolMapper mapper;
    private final AuditService audit;

    public ExamResultService(ExamResultRepository results, EnrollmentRepository enrollments,
                             ClassroomRepository classrooms, SchoolMapper mapper, AuditService audit) {
        this.results = results;
        this.enrollments = enrollments;
        this.classrooms = classrooms;
        this.mapper = mapper;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ExamResultResponse> listForStudent(Long studentId) {
        return results.findByStudentIdOrderByExamDateDesc(studentId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ExamResultResponse> listForClassroom(Long classroomId, CurrentUser viewer) {
        Classroom classroom = classrooms.findById(classroomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        assertCanManageClassroom(classroom, viewer);
        List<Long> studentIds = enrollments.findByClassroomId(classroomId).stream()
                .map(Enrollment::getStudentId).toList();
        if (studentIds.isEmpty()) return List.of();
        return results.findByStudentIdInOrderByExamDateDesc(studentIds).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExamResultResponse upsert(UpsertExamResultRequest req, CurrentUser viewer) {
        assertCanGradeStudent(req.studentId(), viewer);
        ExamResult result = results.findByStudentIdOrderByExamDateDesc(req.studentId()).stream()
                .filter(r -> r.getSubject().equalsIgnoreCase(req.subject())
                        && r.getExamName().equalsIgnoreCase(req.examName()))
                .findFirst()
                .orElseGet(ExamResult::new);
        result.setStudentId(req.studentId());
        result.setSubject(req.subject());
        result.setExamName(req.examName());
        result.setScore(req.score());
        result.setMaxScore(req.maxScore() == null ? 100 : req.maxScore());
        result.setExamDate(req.examDate());
        result.setRecordedBy(viewer.userId());
        ExamResult saved = results.save(result);
        audit.record(AuditEvent.action(AuditAction.EXAM_RESULT_UPSERT).success()
                .user(viewer.username()).role(viewer.role())
                .detail("studentId", req.studentId()).detail("subject", req.subject()));
        return toResponse(saved);
    }

    private void assertCanManageClassroom(Classroom classroom, CurrentUser viewer) {
        if (RoleType.ADMIN.name().equals(viewer.role())) return;
        if (RoleType.TEACHER.name().equals(viewer.role()) && classroom.getTeacherId() != null
                && classroom.getTeacherId().equals(viewer.userId())) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    /** A teacher may grade results of students enrolled in a class they own. */
    private void assertCanGradeStudent(Long studentId, CurrentUser viewer) {
        if (RoleType.ADMIN.name().equals(viewer.role())) return;
        if (RoleType.TEACHER.name().equals(viewer.role())) {
            boolean owns = enrollments.findByStudentId(studentId).stream()
                    .map(Enrollment::getClassroomId)
                    .map(classrooms::findById)
                    .anyMatch(c -> c.isPresent() && viewer.userId().equals(c.get().getTeacherId()));
            if (owns) return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private ExamResultResponse toResponse(ExamResult r) {
        return new ExamResultResponse(r.getId(), r.getStudentId(), mapper.nameOf(r.getStudentId()),
                r.getSubject(), r.getExamName(), r.getScore(), r.getMaxScore(), r.getExamDate());
    }
}

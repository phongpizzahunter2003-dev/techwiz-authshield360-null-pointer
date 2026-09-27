package com.authshield360.school;

import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.school.dto.ExamResultResponse;
import com.authshield360.school.dto.StudentDetailResponse;
import com.authshield360.school.dto.SubmissionResponse;
import com.authshield360.security.CurrentUser;
import com.authshield360.user.RoleType;
import com.authshield360.user.User;
import com.authshield360.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Drill-down for a teacher (or admin) into one student: profile, exam results and the
 * submissions belonging to assignments in the classes that teacher owns.
 * Authorisation is enforced here (BR-05) — a teacher can only read students of their classes.
 */
@Service
public class TeacherStudentService {

    private final UserRepository users;
    private final StudentProfileRepository studentProfiles;
    private final EnrollmentRepository enrollments;
    private final ClassroomRepository classrooms;
    private final ExamResultRepository examResults;
    private final AssignmentRepository assignments;
    private final AssignmentSubmissionRepository submissions;
    private final SchoolMapper mapper;

    public TeacherStudentService(UserRepository users, StudentProfileRepository studentProfiles,
                                 EnrollmentRepository enrollments, ClassroomRepository classrooms,
                                 ExamResultRepository examResults, AssignmentRepository assignments,
                                 AssignmentSubmissionRepository submissions, SchoolMapper mapper) {
        this.users = users;
        this.studentProfiles = studentProfiles;
        this.enrollments = enrollments;
        this.classrooms = classrooms;
        this.examResults = examResults;
        this.assignments = assignments;
        this.submissions = submissions;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public StudentDetailResponse detail(Long studentId, CurrentUser viewer) {
        User student = users.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (student.getRole() != RoleType.STUDENT) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "This account is not a student.");
        }

        // Classes the viewer may look into.
        List<Classroom> scope = RoleType.ADMIN.name().equals(viewer.role())
                ? classrooms.findAllByOrderByNameAsc()
                : classrooms.findByTeacherIdOrderByNameAsc(viewer.userId());
        Set<Long> scopeIds = scope.stream().map(Classroom::getId).collect(Collectors.toSet());

        List<Long> studentClassIds = enrollments.findByStudentId(studentId).stream()
                .map(Enrollment::getClassroomId).toList();
        boolean allowed = studentClassIds.stream().anyMatch(scopeIds::contains);
        if (!allowed) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        StudentProfile profile = studentProfiles.findByUserId(studentId).orElse(null);

        List<ExamResultResponse> results = examResults.findByStudentIdOrderByExamDateDesc(studentId).stream()
                .map(r -> new ExamResultResponse(r.getId(), r.getStudentId(), student.getFullName(),
                        r.getSubject(), r.getExamName(), r.getScore(), r.getMaxScore(), r.getExamDate()))
                .toList();

        // Submissions, limited to assignments inside the viewer's classes (avoids N+1).
        List<AssignmentSubmission> own = submissions.findByStudentIdOrderBySubmittedAtDesc(studentId);
        Set<Long> assignmentIds = own.stream().map(AssignmentSubmission::getAssignmentId).collect(Collectors.toSet());
        Map<Long, Assignment> assignmentById = assignments.findAllById(assignmentIds).stream()
                .collect(Collectors.toMap(Assignment::getId, Function.identity()));

        List<SubmissionResponse> submissionResponses = own.stream()
                .filter(s -> {
                    Assignment a = assignmentById.get(s.getAssignmentId());
                    return a != null && scopeIds.contains(a.getClassroomId());
                })
                .map(s -> mapper.toSubmission(s, assignmentById.get(s.getAssignmentId()), student.getFullName(), true))
                .toList();

        return new StudentDetailResponse(
                student.getId(),
                student.getUsername(),
                student.getFullName(),
                student.getEmail(),
                student.getPhone(),
                profile == null ? null : profile.getStudentCode(),
                profile == null ? null : profile.getClassName(),
                scope.stream()
                        .filter(c -> studentClassIds.contains(c.getId()))
                        .map(Classroom::getName)
                        .toList(),
                results,
                submissionResponses);
    }
}

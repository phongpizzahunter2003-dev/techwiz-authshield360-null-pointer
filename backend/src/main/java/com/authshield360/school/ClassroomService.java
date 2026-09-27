package com.authshield360.school;

import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.school.dto.ClassroomResponse;
import com.authshield360.school.dto.CreateClassroomRequest;
import com.authshield360.security.CurrentUser;
import com.authshield360.user.RoleType;
import com.authshield360.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Classroom management and enrollment. */
@Service
public class ClassroomService {

    private final ClassroomRepository classrooms;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final SchoolMapper mapper;

    public ClassroomService(ClassroomRepository classrooms, EnrollmentRepository enrollments,
                            UserRepository users, SchoolMapper mapper) {
        this.classrooms = classrooms;
        this.enrollments = enrollments;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> listFor(CurrentUser viewer) {
        List<Classroom> result;
        switch (RoleType.valueOf(viewer.role())) {
            case ADMIN -> result = classrooms.findAllByOrderByNameAsc();
            case TEACHER -> result = classrooms.findByTeacherIdOrderByNameAsc(viewer.userId());
            case STUDENT -> {
                List<Long> ids = enrollments.findByStudentId(viewer.userId()).stream()
                        .map(Enrollment::getClassroomId).toList();
                result = ids.isEmpty() ? List.of() : classrooms.findAllById(ids);
            }
            default -> result = List.of();
        }
        return result.stream().map(mapper::toClassroom).toList();
    }

    @Transactional(readOnly = true)
    public Classroom getOrThrow(Long id) {
        return classrooms.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Classroom assertCanManage(Long classroomId, CurrentUser viewer) {
        Classroom classroom = getOrThrow(classroomId);
        if (RoleType.ADMIN.name().equals(viewer.role())) {
            return classroom;
        }
        if (RoleType.TEACHER.name().equals(viewer.role()) && classroom.getTeacherId() != null
                && classroom.getTeacherId().equals(viewer.userId())) {
            return classroom;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public ClassroomResponse create(CreateClassroomRequest req, CurrentUser viewer) {
        if (classrooms.existsByCode(req.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Mã lớp đã tồn tại.");
        }
        Classroom classroom = new Classroom();
        classroom.setCode(req.code().trim());
        classroom.setName(req.name().trim());
        classroom.setDescription(req.description());
        Long teacherId = req.teacherId();
        if (teacherId == null && RoleType.TEACHER.name().equals(viewer.role())) {
            teacherId = viewer.userId();
        }
        classroom.setTeacherId(teacherId);
        return mapper.toClassroom(classrooms.save(classroom));
    }

    @Transactional
    public ClassroomResponse update(Long id, CreateClassroomRequest req, CurrentUser viewer) {
        Classroom classroom = assertCanManage(id, viewer);
        classroom.setName(req.name().trim());
        classroom.setDescription(req.description());
        if (req.teacherId() != null) {
            classroom.setTeacherId(req.teacherId());
        }
        return mapper.toClassroom(classrooms.save(classroom));
    }

    @Transactional
    public void delete(Long id, CurrentUser viewer) {
        assertCanManage(id, viewer);
        enrollments.findByClassroomId(id).forEach(enrollments::delete);
        classrooms.deleteById(id);
    }

    @Transactional
    public void enroll(Long classroomId, Long studentId, CurrentUser viewer) {
        Classroom classroom = assertCanManage(classroomId, viewer);
        var student = users.findById(studentId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (student.getRole() != RoleType.STUDENT) {
            throw new BusinessException(ErrorCode.CONFLICT, "Chỉ có thể thêm học sinh vào lớp.");
        }
        if (!enrollments.existsByClassroomIdAndStudentId(classroom.getId(), studentId)) {
            enrollments.save(new Enrollment(classroom.getId(), studentId));
        }
    }

    @Transactional
    public void unenroll(Long classroomId, Long studentId, CurrentUser viewer) {
        assertCanManage(classroomId, viewer);
        enrollments.deleteByClassroomIdAndStudentId(classroomId, studentId);
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> listManagedBy(CurrentUser viewer) {
        return listFor(viewer);
    }

    /** Student directory for teacher enrollment UIs (id/name only needed client-side). */
    @Transactional(readOnly = true)
    public List<com.authshield360.user.dto.UserResponse> listStudents() {
        return users.findByRoleOrderByUsernameAsc(RoleType.STUDENT).stream()
                .map(com.authshield360.user.UserService::toResponse)
                .toList();
    }

    /** Students enrolled in a class the teacher owns (drill-down detail page). */
    @Transactional(readOnly = true)
    public List<com.authshield360.user.dto.UserResponse> listStudentsInClassroom(Long classroomId, CurrentUser viewer) {
        assertCanManage(classroomId, viewer);
        List<Long> ids = enrollments.findByClassroomId(classroomId).stream()
                .map(Enrollment::getStudentId).toList();
        if (ids.isEmpty()) return List.of();
        return users.findAllById(ids).stream()
                .map(com.authshield360.user.UserService::toResponse)
                .toList();
    }
}

package com.authshield360.school;

import com.authshield360.common.ApiResponse;
import com.authshield360.school.dto.ClassroomResponse;
import com.authshield360.school.dto.CreateClassroomRequest;
import com.authshield360.school.dto.EnrollStudentRequest;
import com.authshield360.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    /** Role-aware list: student → enrolled, teacher → owned, admin → all. */
    @GetMapping("/classrooms")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<List<ClassroomResponse>> list() {
        return ApiResponse.ok(classroomService.listFor(SecurityUtils.current()));
    }

    /** Student directory (teacher/admin) used by the enrollment UI. */
    @GetMapping("/teacher/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<com.authshield360.user.dto.UserResponse>> students() {
        return ApiResponse.ok(classroomService.listStudents());
    }

    /** Students enrolled in a specific class (drill-down detail page). */
    @GetMapping("/teacher/classrooms/{id}/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<com.authshield360.user.dto.UserResponse>> classroomStudents(@PathVariable Long id) {
        return ApiResponse.ok(classroomService.listStudentsInClassroom(id, SecurityUtils.current()));
    }

    @PostMapping("/teacher/classrooms")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ClassroomResponse> create(@Valid @RequestBody CreateClassroomRequest request) {
        return ApiResponse.ok("Class created.", classroomService.create(request, SecurityUtils.current()));
    }

    @PutMapping("/teacher/classrooms/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ClassroomResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody CreateClassroomRequest request) {
        return ApiResponse.ok("Class updated.",
                classroomService.update(id, request, SecurityUtils.current()));
    }

    @DeleteMapping("/teacher/classrooms/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        classroomService.delete(id, SecurityUtils.current());
        return ApiResponse.ok("Class deleted.", null);
    }

    @PostMapping("/teacher/classrooms/{id}/enroll")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> enroll(@PathVariable Long id, @Valid @RequestBody EnrollStudentRequest request) {
        classroomService.enroll(id, request.studentId(), SecurityUtils.current());
        return ApiResponse.ok("Student added to the class.", null);
    }

    @DeleteMapping("/teacher/classrooms/{id}/students/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> unenroll(@PathVariable Long id, @PathVariable Long studentId) {
        classroomService.unenroll(id, studentId, SecurityUtils.current());
        return ApiResponse.ok("Student removed from the class.", null);
    }
}

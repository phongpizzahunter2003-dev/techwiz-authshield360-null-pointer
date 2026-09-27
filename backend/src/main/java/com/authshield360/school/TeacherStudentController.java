package com.authshield360.school;

import com.authshield360.common.ApiResponse;
import com.authshield360.school.dto.StudentDetailResponse;
import com.authshield360.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Teacher/admin drill-down into a single student of their classes. */
@RestController
@RequestMapping("/api/v1/teacher/students")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;

    public TeacherStudentController(TeacherStudentService teacherStudentService) {
        this.teacherStudentService = teacherStudentService;
    }

    @GetMapping("/{studentId}")
    public ApiResponse<StudentDetailResponse> detail(@PathVariable Long studentId) {
        return ApiResponse.ok(teacherStudentService.detail(studentId, SecurityUtils.current()));
    }
}

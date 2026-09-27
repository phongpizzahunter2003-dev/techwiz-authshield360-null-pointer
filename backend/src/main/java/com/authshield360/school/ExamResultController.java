package com.authshield360.school;

import com.authshield360.common.ApiResponse;
import com.authshield360.school.dto.ExamResultResponse;
import com.authshield360.school.dto.UpsertExamResultRequest;
import com.authshield360.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ExamResultController {

    private final ExamResultService examResultService;

    public ExamResultController(ExamResultService examResultService) {
        this.examResultService = examResultService;
    }

    /** Student views only their own results. */
    @GetMapping("/student/results")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<ExamResultResponse>> myResults() {
        return ApiResponse.ok(examResultService.listForStudent(SecurityUtils.current().userId()));
    }

    @GetMapping("/teacher/classrooms/{classroomId}/results")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<ExamResultResponse>> classroomResults(@PathVariable Long classroomId) {
        return ApiResponse.ok(examResultService.listForClassroom(classroomId, SecurityUtils.current()));
    }

    @PostMapping("/teacher/results")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ExamResultResponse> upsert(@Valid @RequestBody UpsertExamResultRequest request) {
        return ApiResponse.ok("Đã lưu kết quả thi.", examResultService.upsert(request, SecurityUtils.current()));
    }
}

package com.authshield360.school;

import com.authshield360.common.ApiResponse;
import com.authshield360.school.dto.AssignmentResponse;
import com.authshield360.school.dto.CreateAssignmentRequest;
import com.authshield360.school.dto.UpdateAssignmentRequest;
import com.authshield360.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /** Role-aware list (UC-05): student sees own classes, teacher own, admin all. */
    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<List<AssignmentResponse>> list() {
        return ApiResponse.ok(assignmentService.listFor(SecurityUtils.current()));
    }

    @GetMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<AssignmentResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(assignmentService.getFor(id, SecurityUtils.current()));
    }

    @PostMapping("/teacher/assignments")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request) {
        return ApiResponse.ok("Tạo bài tập thành công.", assignmentService.create(request, SecurityUtils.current()));
    }

    @PutMapping("/teacher/assignments/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<AssignmentResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateAssignmentRequest request) {
        return ApiResponse.ok("Cập nhật bài tập thành công.",
                assignmentService.update(id, request, SecurityUtils.current()));
    }

    @PostMapping("/teacher/assignments/{id}/close")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<AssignmentResponse> close(@PathVariable Long id) {
        return ApiResponse.ok("Đã đóng bài tập.", assignmentService.close(id, SecurityUtils.current()));
    }
}

package com.authshield360.school;

import com.authshield360.common.ApiResponse;
import com.authshield360.school.dto.GradeRequest;
import com.authshield360.school.dto.SubmissionResponse;
import com.authshield360.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /** UC-A1/A2/A4 — submit or resubmit (multipart). UC-A3 rejections return 409. */
    @PostMapping("/student/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SubmissionResponse> submit(@PathVariable Long assignmentId,
                                                  @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Submission accepted.", submissionService.submit(assignmentId, file, SecurityUtils.current()));
    }

    /** Own attempt history (UC-A4c). */
    @GetMapping("/student/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<SubmissionResponse>> history(@PathVariable Long assignmentId) {
        return ApiResponse.ok(submissionService.history(assignmentId, SecurityUtils.current()));
    }

    /** All of the current student's submissions. */
    @GetMapping("/student/submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<SubmissionResponse>> mySubmissions() {
        return ApiResponse.ok(submissionService.listForStudent(SecurityUtils.current()));
    }

    /** Teacher view of all submissions for an assignment. */
    @GetMapping("/teacher/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<SubmissionResponse>> forAssignment(@PathVariable Long assignmentId) {
        return ApiResponse.ok(submissionService.listForAssignment(assignmentId, SecurityUtils.current()));
    }

    @PostMapping("/teacher/submissions/{id}/grade")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<SubmissionResponse> grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return ApiResponse.ok("Submission graded.", submissionService.grade(id, request, SecurityUtils.current()));
    }

    /** Download a stored file — ownership checked server-side (BR-05). */
    @GetMapping("/submissions/{id}/file")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        SubmissionService.SubmissionDownload download = submissionService.download(id, SecurityUtils.current());
        String filename = download.filename() == null ? "submission" : download.filename();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(download.contentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(download.contentType()))
                .body(download.resource());
    }
}

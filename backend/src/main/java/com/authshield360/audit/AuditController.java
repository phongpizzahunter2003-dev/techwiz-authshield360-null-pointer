package com.authshield360.audit;

import com.authshield360.audit.dto.AuditLogResponse;
import com.authshield360.common.ApiResponse;
import com.authshield360.common.PageResult;
import com.authshield360.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/** Admin-only audit log viewer + masked export (UC-11). */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditQueryService queryService;
    private final AuditExportService exportService;
    private final AuditService audit;

    public AuditController(AuditQueryService queryService, AuditExportService exportService, AuditService audit) {
        this.queryService = queryService;
        this.exportService = exportService;
        this.audit = audit;
    }

    @GetMapping
    public ApiResponse<PageResult<AuditLogResponse>> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int safeSize = Math.min(Math.max(size, 1), 50); // UC-11: max 50 rows per page
        AuditFilter filter = new AuditFilter(role, action, status, mode, query, from, to);
        Page<AuditLog> result = queryService.page(filter,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "eventTime")));

        SecurityUtils.currentOrEmpty().ifPresent(cu ->
                audit.record(AuditEvent.action(AuditAction.LOG_VIEW).success()
                        .user(cu.username()).role(cu.role())
                        .detail("page", page).detail("total", result.getTotalElements())));

        return ApiResponse.ok(PageResult.of(result, queryService::toResponse));
    }

    /** Single audit row for the detail view (drill-down from the dashboard chart). */
    @GetMapping("/{id}")
    public ApiResponse<AuditLogResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(queryService.toResponse(queryService.byId(id)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        AuditFilter filter = new AuditFilter(role, action, status, mode, query, from, to);
        AuditExportService.ExportedFile file = exportService.export(filter, format);

        SecurityUtils.currentOrEmpty().ifPresent(cu ->
                audit.record(AuditEvent.action(AuditAction.LOG_EXPORT).success()
                        .user(cu.username()).role(cu.role())
                        .detail("format", format).detail("total", file.totalMatching())
                        .detail("truncated", file.truncated())));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .header("X-Export-Total", String.valueOf(file.totalMatching()))
                .header("X-Export-Truncated", String.valueOf(file.truncated()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}

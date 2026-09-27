package com.authshield360.system;

import com.authshield360.common.ApiResponse;
import com.authshield360.system.dto.DbStatusResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin diagnostics: confirms the live database and per-table row counts. */
@RestController
@RequestMapping("/api/v1/admin/system")
@PreAuthorize("hasRole('ADMIN')")
public class SystemController {

    private final DbStatusService dbStatusService;

    public SystemController(DbStatusService dbStatusService) {
        this.dbStatusService = dbStatusService;
    }

    @GetMapping("/db-status")
    public ApiResponse<DbStatusResponse> dbStatus() {
        return ApiResponse.ok(dbStatusService.status());
    }
}

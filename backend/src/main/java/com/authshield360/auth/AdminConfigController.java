package com.authshield360.auth;

import com.authshield360.auth.dto.AuthConfigResponse;
import com.authshield360.auth.dto.UpdateConfigRequest;
import com.authshield360.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin-only auth/MFA configuration (UC-08). */
@RestController
@RequestMapping("/api/v1/admin/config")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

    private final ConfigService configService;

    public AdminConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ApiResponse<AuthConfigResponse> get() {
        return ApiResponse.ok(configService.toResponse(configService.current()));
    }

    @PutMapping
    public ApiResponse<AuthConfigResponse> update(@Valid @RequestBody UpdateConfigRequest request) {
        return ApiResponse.ok("Cấu hình đã được lưu. Hệ thống sẽ áp dụng ở lần đăng nhập tiếp theo.",
                configService.update(request));
    }
}

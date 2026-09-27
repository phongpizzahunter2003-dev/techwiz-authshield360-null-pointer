package com.authshield360.user;

import com.authshield360.common.ApiResponse;
import com.authshield360.common.PageResult;
import com.authshield360.user.dto.CreateUserRequest;
import com.authshield360.user.dto.UpdateUserRequest;
import com.authshield360.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin-only user & role management (UC-07). Guarded by URL rule + @PreAuthorize (BR-05). */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResult<UserResponse>> list(
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        var pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
        return ApiResponse.ok(userService.search(role, q, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok("Account created.", userService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok("Account updated.", userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok("Account deleted.", null);
    }

    @PostMapping("/{id}/reset-mfa")
    public ApiResponse<Void> resetMfa(@PathVariable Long id) {
        userService.resetMfa(id);
        return ApiResponse.ok("MFA has been reset.", null);
    }

    /**
     * Bulk security-policy assignment (admin): apply S1 / S2 / S3 to every user or to one role.
     * MFA enrolment itself remains self-service (UC-09).
     */
    @PostMapping("/apply-auth-mode")
    public ApiResponse<java.util.Map<String, Object>> applyAuthMode(
            @Valid @RequestBody com.authshield360.user.dto.ApplyAuthModeRequest request) {
        int affected = userService.applyAuthMode(request.mode(), request.role());
        String scope = request.role() == null ? "all users" : request.role().name();
        return ApiResponse.ok("Applied mode " + request.mode() + " to " + affected + " account(s) (" + scope + ").",
                java.util.Map.of("affected", affected, "mode", request.mode(),
                        "scope", request.role() == null ? "ALL" : request.role().name()));
    }
}

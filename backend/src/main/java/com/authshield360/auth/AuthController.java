package com.authshield360.auth;

import com.authshield360.auth.dto.*;
import com.authshield360.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints (UC-01..UC-04, UC-06, UC-09).
 * Element/parameter names mirror the functional spec (e.g. resend-otp).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** UC-01/02/03 step 1. */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** UC-02/03 step 2 (and step 3 for S3). */
    @PostMapping("/otp/verify")
    public ApiResponse<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ApiResponse.ok(authService.verifyOtp(request));
    }

    /** UC-04 resend OTP (server-side 60s throttle → 429). */
    @PostMapping("/resend-otp")
    public ApiResponse<LoginResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ApiResponse.ok(authService.resendOtp(request));
    }

    /** UC-06 logout + session invalidation. */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok("You have signed out successfully.", null);
    }

    /** Current session details (used by the SPA to hydrate auth state). */
    @GetMapping("/session")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SessionResponse> session() {
        return ApiResponse.ok(authService.session());
    }

    /** UC-09 enrollment start — returns the TOTP secret + otpauth URI. */
    @PostMapping("/mfa/enroll")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MfaEnrollResponse> enroll() {
        return ApiResponse.ok(authService.enrollStart());
    }

    /** UC-09 enrollment confirmation. */
    @PostMapping("/mfa/enroll/confirm")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> confirmEnroll(@Valid @RequestBody ConfirmMfaRequest request) {
        authService.enrollConfirm(request);
        return ApiResponse.ok("Two-factor authentication has been enabled.", null);
    }
}

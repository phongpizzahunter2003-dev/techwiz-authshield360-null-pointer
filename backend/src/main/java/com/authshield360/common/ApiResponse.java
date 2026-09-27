package com.authshield360.common;

import java.time.Instant;

/** Uniform API envelope (be-rules.md §3). */
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp,
        String correlationId
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "Success", data, Instant.now(), Correlation.current());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data, Instant.now(), Correlation.current());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, Instant.now(), Correlation.current());
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data, Instant.now(), Correlation.current());
    }
}

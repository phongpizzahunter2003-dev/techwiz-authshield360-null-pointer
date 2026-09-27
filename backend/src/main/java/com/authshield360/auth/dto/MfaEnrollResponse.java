package com.authshield360.auth.dto;

/** UC-09 enrollment start payload. */
public record MfaEnrollResponse(String secret, String otpauthUri) {
}

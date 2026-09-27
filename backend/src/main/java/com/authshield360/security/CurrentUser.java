package com.authshield360.security;

/** Authenticated principal placed in the SecurityContext. */
public record CurrentUser(
        Long userId,
        String username,
        String role,
        String sessionId
) {
}

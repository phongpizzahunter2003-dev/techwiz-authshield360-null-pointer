package com.authshield360.security;

import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Convenience accessors for the authenticated principal. */
public final class SecurityUtils {

    private SecurityUtils() { }

    public static Optional<CurrentUser> currentOrEmpty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser cu) {
            return Optional.of(cu);
        }
        return Optional.empty();
    }

    public static CurrentUser current() {
        return currentOrEmpty().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    }
}

package com.authshield360.security;

import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditEvent;
import com.authshield360.audit.AuditService;
import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Validates the Bearer access token and cross-checks the server-side session (AD-1, BR-08).
 * Detects revoked-session replay and records it (UC-06).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final SessionService sessionService;
    private final AuditService auditService;

    public JwtAuthFilter(TokenService tokenService, SessionService sessionService, AuditService auditService) {
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            tryAuthenticate(token);
        }
        chain.doFilter(request, response);
    }

    private void tryAuthenticate(String token) {
        final TokenClaims claims;
        try {
            claims = tokenService.parse(token);
        } catch (BusinessException e) {
            return; // unauthenticated -> entry point returns 401
        }
        if (!TokenClaims.PURPOSE_ACCESS.equals(claims.purpose()) || claims.sessionId() == null) {
            return; // challenge tokens are not valid API credentials
        }
        Optional<SessionRecord> maybe = sessionService.find(claims.sessionId());
        if (maybe.isEmpty()) {
            audit(AuditAction.SESSION_REPLAY_ATTEMPT, claims, "INVALID_SESSION");
            return;
        }
        SessionRecord session = maybe.get();
        if (!session.isActive()) {
            audit(AuditAction.SESSION_REPLAY_ATTEMPT, claims, "INVALID_SESSION");
            return;
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            sessionService.expire(session.getSessionId());
            audit(AuditAction.SESSION_EXPIRED, claims, null);
            return;
        }
        sessionService.touch(session.getSessionId());
        CurrentUser principal = new CurrentUser(claims.userId(), claims.username(), claims.role(), claims.sessionId());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void audit(String action, TokenClaims claims, String reason) {
        AuditEvent event = AuditEvent.action(action)
                .user(claims.username())
                .role(claims.role())
                .session(claims.sessionId());
        if (reason != null) {
            event.failure(reason);
        } else {
            event.success();
        }
        auditService.record(event);
    }
}

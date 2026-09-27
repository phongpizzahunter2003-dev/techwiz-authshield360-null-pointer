package com.authshield360.security;

import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditEvent;
import com.authshield360.audit.AuditService;
import com.authshield360.common.ApiResponse;
import com.authshield360.common.ErrorCode;
import com.authshield360.common.MediaTypes;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns JSON 403 and records PRIVILEGE_VIOLATION (UC-05, BR-05). */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, AuditService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        SecurityUtils.currentOrEmpty().ifPresent(cu -> auditService.record(
                AuditEvent.action(AuditAction.PRIVILEGE_VIOLATION)
                        .failure("ACCESS_DENIED")
                        .user(cu.username())
                        .role(cu.role())
                        .session(cu.sessionId())
                        .detail("requestedUrl", request.getRequestURI())));

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaTypes.JSON);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.ACCESS_DENIED.name(), ErrorCode.ACCESS_DENIED.message());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

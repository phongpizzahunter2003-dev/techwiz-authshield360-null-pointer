package com.authshield360.security;

import com.authshield360.common.ApiResponse;
import com.authshield360.common.ErrorCode;
import com.authshield360.common.MediaTypes;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns the uniform JSON envelope on 401 instead of a redirect/HTML page. */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaTypes.JSON);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.UNAUTHENTICATED.name(), ErrorCode.UNAUTHENTICATED.message());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

package com.authshield360.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Reads client ip / user-agent from the current request for auditing (BR-07). */
public final class WebUtils {

    private WebUtils() { }

    public static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    public static String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "unknown";
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real;
        return req.getRemoteAddr();
    }

    public static String userAgent() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "unknown";
        String ua = req.getHeader("User-Agent");
        if (ua == null) return "unknown";
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }

    public static String requestedUrl() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String uri = req.getRequestURI();
        String q = req.getQueryString();
        String full = q == null ? uri : uri + "?" + q;
        return full.length() > 500 ? full.substring(0, 500) : full;
    }
}

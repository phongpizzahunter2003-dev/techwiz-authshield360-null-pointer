package com.authshield360.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Externalised (non-secret) application configuration.
 * Secrets (signing key, SMTP/DB passwords) come from environment variables only (BR-10).
 */
@ConfigurationProperties(prefix = "authshield")
public class AppProperties {

    /** HMAC signing key. MUST be provided via env in non-dev profiles. */
    private String signingKey = "dev-only-signing-key-change-me-please-32bytes";

    /** Access token TTL in minutes. */
    private long tokenTtlMinutes = 60;

    /** Session idle timeout in minutes (UC-06). */
    private long sessionTimeoutMinutes = 30;

    /** Directory where uploaded assignment files are stored. */
    private String uploadDir = "uploads";

    /** Max upload size in bytes (default 10 MB). */
    private long maxUploadBytes = 10L * 1024 * 1024;

    /** Allowed CORS origins. */
    private List<String> corsAllowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173");

    /** Development convenience: expose the generated OTP in API responses / logs (VD-06). */
    private boolean exposeOtp = true;

    public String getSigningKey() { return signingKey; }
    public void setSigningKey(String signingKey) { this.signingKey = signingKey; }
    public long getTokenTtlMinutes() { return tokenTtlMinutes; }
    public void setTokenTtlMinutes(long v) { this.tokenTtlMinutes = v; }
    public long getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(long v) { this.sessionTimeoutMinutes = v; }
    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    public long getMaxUploadBytes() { return maxUploadBytes; }
    public void setMaxUploadBytes(long v) { this.maxUploadBytes = v; }
    public List<String> getCorsAllowedOrigins() { return corsAllowedOrigins; }
    public void setCorsAllowedOrigins(List<String> v) { this.corsAllowedOrigins = v; }
    public boolean isExposeOtp() { return exposeOtp; }
    public void setExposeOtp(boolean exposeOtp) { this.exposeOtp = exposeOtp; }
}

package com.authshield360.audit;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Removes/masks sensitive values before they reach logs or exports (BR-02, BR-10, UC-11 §11.3). */
@Component
public class MaskingUtil {

    private static final Pattern EMAIL = Pattern.compile("(?i)([A-Z0-9._%+-]{1,3})[A-Z0-9._%+-]*(@[A-Z0-9.-]+\\.[A-Z]{2,})");
    private static final Pattern PHONE = Pattern.compile("\\b(0\\d{2})(\\d{3})(\\d{3,4})\\b");

    private final ObjectMapper objectMapper;

    public MaskingUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 3) return local + "***" + domain;
        return local.substring(0, 3) + "******" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
    }

    /** Masks PII inside free text (used in exports). */
    public String maskPii(String text) {
        if (text == null) return null;
        String result = EMAIL.matcher(text).replaceAll(m -> m.group(1) + "******" + m.group(2));
        result = PHONE.matcher(result).replaceAll("$1***$3");
        return result;
    }

    /** Hard-scrubs secrets so they can never be persisted (BR-02/BR-10). */
    public String scrub(String text) {
        if (text == null) return null;
        String result = text
                .replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"otp\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"code\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"secret\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"token\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
        return result;
    }

    /** Serialises a detail map safely (scrubbed, truncated). */
    public String toDetail(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) return null;
        try {
            Map<String, Object> safe = new LinkedHashMap<>(detail);
            String json = scrub(objectMapper.writeValueAsString(safe));
            return json.length() > 1900 ? json.substring(0, 1900) : json;
        } catch (Exception e) {
            return null;
        }
    }
}

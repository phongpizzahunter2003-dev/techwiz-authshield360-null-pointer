package com.authshield360.audit;

import com.authshield360.auth.AuthConfig;
import com.authshield360.auth.ConfigService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports audit logs as CSV or JSON with PII masking and a hard 10,000-row cap (UC-11 §11.3).
 * File naming: auth_logs_[MODE]_[YYYYMMDD]_[HHMMSS].[ext]
 */
@Service
public class AuditExportService {

    public static final int EXPORT_LIMIT = 10_000;

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneOffset.UTC);

    private final AuditQueryService queryService;
    private final MaskingUtil masking;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    public AuditExportService(AuditQueryService queryService, MaskingUtil masking,
                              ConfigService configService, ObjectMapper objectMapper) {
        this.queryService = queryService;
        this.masking = masking;
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    public record ExportedFile(byte[] content, String filename, String contentType, long totalMatching,
                               boolean truncated) { }

    @Transactional(readOnly = true)
    public ExportedFile export(AuditFilter filter, String format) {
        long total = queryService.count(filter);
        boolean truncated = total > EXPORT_LIMIT;
        List<AuditLog> rows = queryService.list(filter,
                PageRequest.of(0, EXPORT_LIMIT, Sort.by(Sort.Direction.DESC, "eventTime")));

        AuthConfig config = configService.current();
        String stamp = STAMP.format(java.time.Instant.now());
        String base = "auth_logs_" + config.getMode().name() + "_" + stamp;

        if ("json".equalsIgnoreCase(format)) {
            return new ExportedFile(toJson(rows), base + ".json", "application/json", total, truncated);
        }
        return new ExportedFile(toCsv(rows), base + ".csv", "text/csv;charset=UTF-8", total, truncated);
    }

    private byte[] toJson(List<AuditLog> rows) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (AuditLog log : rows) {
            payload.add(toMaskedMap(log));
        }
        return objectMapper.writeValueAsBytes(payload);
    }

    private Map<String, Object> toMaskedMap(AuditLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", log.getEventTime() == null ? null : log.getEventTime().toString());
        map.put("event_id", log.getEventId());
        map.put("event_action", log.getEventAction());
        map.put("status", log.getStatus());
        map.put("auth_factor", log.getAuthFactor());
        map.put("auth_mode", log.getAuthMode());
        map.put("user_identifier", masking.maskPii(log.getUserIdentifier()));
        map.put("role", log.getRole());
        map.put("client_ip", masking.maskPii(log.getClientIp()));
        map.put("session_id", log.getSessionId());
        map.put("failure_reason", log.getFailureReason());
        map.put("correlation_id", log.getCorrelationId());
        map.put("detail", masking.maskPii(log.getDetail()));
        return map;
    }

    private byte[] toCsv(List<AuditLog> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,event_id,event_action,status,auth_factor,auth_mode,user_identifier,role,")
                .append("client_ip,session_id,failure_reason,correlation_id,detail\n");
        for (AuditLog log : rows) {
            sb.append(csv(log.getEventTime() == null ? null : log.getEventTime().toString())).append(',')
                    .append(csv(log.getEventId())).append(',')
                    .append(csv(log.getEventAction())).append(',')
                    .append(csv(log.getStatus())).append(',')
                    .append(csv(log.getAuthFactor())).append(',')
                    .append(csv(log.getAuthMode())).append(',')
                    .append(csv(masking.maskPii(log.getUserIdentifier()))).append(',')
                    .append(csv(log.getRole())).append(',')
                    .append(csv(masking.maskPii(log.getClientIp()))).append(',')
                    .append(csv(log.getSessionId())).append(',')
                    .append(csv(log.getFailureReason())).append(',')
                    .append(csv(log.getCorrelationId())).append(',')
                    .append(csv(masking.maskPii(log.getDetail()))).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}

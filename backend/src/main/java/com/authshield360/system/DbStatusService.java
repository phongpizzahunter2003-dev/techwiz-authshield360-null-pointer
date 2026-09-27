package com.authshield360.system;

import com.authshield360.system.dto.DbStatusResponse;
import com.authshield360.system.dto.DbTableStatus;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;

/**
 * Answers "is the data actually reaching the database?" by reporting the live JDBC
 * connection metadata and the row count of every table.
 *
 * <p>Security: the table list is a fixed whitelist (no user input reaches the SQL), and the
 * JDBC URL is sanitised so credentials are never returned (BR-10).
 */
@Service
public class DbStatusService {

    /** Fixed whitelist — never built from user input. */
    private static final List<String> TABLES = List.of(
            "users",
            "student_profiles",
            "teacher_profiles",
            "classrooms",
            "enrollments",
            "assignments",
            "assignment_submissions",
            "exam_results",
            "audit_logs",
            "sessions",
            "otp_tokens",
            "login_attempts",
            "auth_config");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final Environment environment;

    public DbStatusService(JdbcTemplate jdbcTemplate, DataSource dataSource, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Transactional(readOnly = true)
    public DbStatusResponse status() {
        String product = "unknown";
        String version = "unknown";
        String url = "unknown";
        try (Connection connection = dataSource.getConnection()) {
            var meta = connection.getMetaData();
            product = meta.getDatabaseProductName();
            version = meta.getDatabaseProductVersion();
            url = sanitize(meta.getURL());
        } catch (Exception ignored) {
            // fall back to unknown values; the table counts below still tell the story
        }

        List<DbTableStatus> tables = TABLES.stream()
                .map(table -> new DbTableStatus(table, count(table)))
                .toList();
        long total = tables.stream().mapToLong(t -> Math.max(t.rows(), 0)).sum();

        return new DbStatusResponse(
                product,
                version,
                url,
                isPersistent(url),
                String.join(",", environment.getActiveProfiles()),
                tables,
                total,
                Instant.now());
    }

    private long count(String table) {
        try {
            Long value = jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
            return value == null ? 0 : value;
        } catch (Exception e) {
            return -1; // table not present yet
        }
    }

    private String sanitize(String jdbcUrl) {
        if (jdbcUrl == null) return "unknown";
        return jdbcUrl.replaceAll("(?i)(password|pwd)=[^;&]*", "$1=***");
    }

    /** H2 in-memory is not durable across restarts; file/MySQL URLs are. */
    private boolean isPersistent(String jdbcUrl) {
        if (jdbcUrl == null) return false;
        String lower = jdbcUrl.toLowerCase();
        return lower.contains("mysql") || lower.contains("jdbc:h2:file") || lower.contains("postgres");
    }
}

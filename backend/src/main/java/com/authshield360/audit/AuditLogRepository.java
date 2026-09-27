package com.authshield360.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    long countByEventActionAndStatus(String eventAction, String status);

    long countByEventAction(String eventAction);

    @Query("select a.authMode, a.status, count(a) from AuditLog a "
            + "where a.eventAction in ('LOGIN_SUCCESS','LOGIN_FAIL') and a.authMode is not null "
            + "group by a.authMode, a.status")
    List<Object[]> loginCountsByMode();

    @Query("select a.authMode, count(a) from AuditLog a "
            + "where a.eventAction in ('OTP_VERIFY_FAIL','EMAIL_OTP_FAILED','OTP_EXPIRED','EMAIL_OTP_EXPIRED') "
            + "and a.authMode is not null group by a.authMode")
    List<Object[]> otpFailuresByMode();

    /** Lightweight projection for the "events per day" chart. */
    @Query("select a.eventTime from AuditLog a where a.eventTime >= :after")
    List<java.time.Instant> findEventTimesAfter(@Param("after") java.time.Instant after);
}

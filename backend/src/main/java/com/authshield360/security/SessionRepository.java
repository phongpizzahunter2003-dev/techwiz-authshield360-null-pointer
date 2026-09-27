package com.authshield360.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionRecord, Long> {

    Optional<SessionRecord> findBySessionId(String sessionId);

    List<SessionRecord> findByUserIdAndActiveTrue(Long userId);

    @Modifying
    @Query("update SessionRecord s set s.lastSeenAt = :now where s.sessionId = :sid and s.active = true")
    int touch(@Param("sid") String sessionId, @Param("now") Instant now);

    @Modifying
    @Query("update SessionRecord s set s.active = false, s.revokedAt = :now, s.revokeReason = :reason "
            + "where s.userId = :userId and s.active = true")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now, @Param("reason") String reason);
}

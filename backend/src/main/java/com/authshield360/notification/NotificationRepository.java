package com.authshield360.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadOrderByCreatedAtDesc(Long userId, boolean read, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    /** Marks every unread notification of a user as read. */
    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :now "
            + "where n.userId = :userId and n.read = false")
    int markAllReadForUser(@Param("userId") Long userId, @Param("now") Instant now);
}

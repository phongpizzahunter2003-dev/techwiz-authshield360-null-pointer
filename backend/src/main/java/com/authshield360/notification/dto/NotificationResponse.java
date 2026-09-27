package com.authshield360.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String targetUrl,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}

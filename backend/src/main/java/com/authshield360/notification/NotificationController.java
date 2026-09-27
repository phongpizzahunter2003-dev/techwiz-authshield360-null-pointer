package com.authshield360.notification;

import com.authshield360.common.ApiResponse;
import com.authshield360.common.PageResult;
import com.authshield360.notification.dto.NotificationResponse;
import com.authshield360.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Per-user in-app notifications (student / teacher / admin). */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResult<NotificationResponse>> list(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Long userId = SecurityUtils.current().userId();
        return ApiResponse.ok(notificationService.list(userId, unread,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        Long userId = SecurityUtils.current().userId();
        return ApiResponse.ok(Map.of(
                "unread", notificationService.unreadCount(userId),
                "total", notificationService.total(userId)));
    }

    /** UNREAD -> READ */
    @PostMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(id, SecurityUtils.current().userId()));
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead() {
        int updated = notificationService.markAllRead(SecurityUtils.current().userId());
        return ApiResponse.ok("All notifications marked as read.", Map.of("updated", updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        notificationService.delete(id, SecurityUtils.current().userId());
        return ApiResponse.ok("Notification dismissed.", null);
    }
}

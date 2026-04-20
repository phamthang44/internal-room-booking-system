package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.NotificationResponse;
import com.thang.roombooking.entity.Notification;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<List<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        
        Long userId = userDetails.getUser().getId();
        Pageable pageable = PageRequest.of(page - 1, size);
        
        Page<Notification> notificationPage = notificationService.getNotificationsByUser(userId, pageable);
        
        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResult.success(
                content,
                page,
                size,
                notificationPage.getTotalElements()
        ));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Long>> getUnreadCount(
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        
        Long userId = userDetails.getUser().getId();
        long count = notificationService.getUnreadCount(userId);
        
        return ResponseEntity.ok(ApiResult.success(count));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        
        Long userId = userDetails.getUser().getId();
        notificationService.markAsRead(id, userId);
        
        return ResponseEntity.ok(ApiResult.success());
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> markAllAsRead(
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        
        Long userId = userDetails.getUser().getId();
        notificationService.markAllAsRead(userId);
        
        return ResponseEntity.ok(ApiResult.success());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Clear 1 notification");
        Long userId = userDetails.getUser().getId();
        notificationService.delete(id, userId);
        
        return ResponseEntity.ok(ApiResult.success());
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> deleteMultipleNotifications(
            @RequestBody List<Long> ids,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Clear selected notifications");
        Long userId = userDetails.getUser().getId();
        notificationService.deleteMultiple(ids, userId);
        
        return ResponseEntity.ok(ApiResult.success());
    }

    @DeleteMapping("/clear-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> clearAllNotifications(
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Clear all notifications");
        Long userId = userDetails.getUser().getId();
        notificationService.clearAll(userId);
        
        return ResponseEntity.ok(ApiResult.success());
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .relatedId(notification.getRelatedId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

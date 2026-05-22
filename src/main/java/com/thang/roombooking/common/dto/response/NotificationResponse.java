package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.NotificationType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationResponse(
        Long id,
        String title,
        String message,
        NotificationType type,
        boolean isRead,
        String readStatus, // "READ" or "UNREAD" - always derived from isRead
        String relatedId,
        Instant createdAt
) {
    // Compact constructor: readStatus is always derived from isRead,
    // making it impossible for the two fields to be inconsistent.
    public NotificationResponse {
        readStatus = isRead ? "READ" : "UNREAD";
    }
}

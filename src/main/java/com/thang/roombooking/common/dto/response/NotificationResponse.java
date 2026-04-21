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
        String readStatus, // "READ" or "UNREAD" - helps FE design
        String relatedId,
        Instant createdAt
) {
}

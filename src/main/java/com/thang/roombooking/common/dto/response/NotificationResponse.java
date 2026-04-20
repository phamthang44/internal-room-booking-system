package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private String relatedId;
    private Instant createdAt;
}

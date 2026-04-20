package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.model.NotificationPayload;
import com.thang.roombooking.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void notifyUser(String userId, String type, String message);

    void notifyUser(String userId, NotificationPayload payload);

    void sendToTopic(String topic, NotificationPayload payload);

    // --- Persistence Methods ---
    Page<Notification> getNotificationsByUser(Long userId, Pageable pageable);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    long getUnreadCount(Long userId);

    void saveAndPush(Long userId, NotificationPayload payload);

    void delete(Long notificationId, Long userId);

    void deleteMultiple(java.util.List<Long> ids, Long userId);

    void clearAll(Long userId);

    void saveForAdmins(NotificationPayload payload);
}

package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.model.NotificationPayload;
import com.thang.roombooking.common.enums.NotificationType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import com.thang.roombooking.entity.Notification;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.NotificationRepository;
import com.thang.roombooking.repository.UserAccountRepository;
import com.thang.roombooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public void notifyUser(String userId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                new NotificationPayload(type, message, message, null, null, Instant.now())
        );
    }

    @Override
    public void notifyUser(String userId, NotificationPayload payload) {
        log.info("Sending WS notification to user {} | type={} | bookingId={}", 
                userId, payload.type(), payload.bookingId());
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                payload
        );
    }

    @Override
    public void sendToTopic(String topic, NotificationPayload payload) {
        log.info("Broadcasting WS notification to topic {} | type={} | bookingId={}",
                topic, payload.type(), payload.bookingId());
        messagingTemplate.convertAndSend(topic, payload);
    }

    // --- Persistence Implementation ---

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsByUser(Long userId, Boolean isRead, Pageable pageable) {
        Page<Notification> page = notificationRepository.findAllByUserIdAndIsRead(userId, isRead, pageable);
        // Translate messages on-the-fly based on the requesting user's current locale
        page.forEach(this::resolveI18n);
        return page;
    }

    private void resolveI18n(Notification n) {
        n.setTitle(resolveText(n.getTitle(), ".title"));
        n.setMessage(resolveText(n.getMessage(), ".message"));
    }

    private String resolveText(String storedValue, String suffix) {
        if (storedValue != null && storedValue.startsWith("I18N:")) {
            try {
                String json = storedValue.substring(5);
                I18nData data = objectMapper.readValue(json, I18nData.class);
                return I18nUtils.get(data.key() + suffix, data.params());
            } catch (Exception e) {
                log.warn("Failed to resolve i18n for: {}", storedValue);
            }
        }
        return storedValue;
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            log.warn("Notification {} not found or not owned by user {}", notificationId, userId);
            return;
        }
        // Push WebSocket event so the frontend updates its local read-state in real-time
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            resolveI18n(notification);
            notifyUser(String.valueOf(userId),
                    NotificationPayload.fromNotification(notification, notification.getTitle(), notification.getMessage()));
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
        // Signal the frontend to clear all unread badges in one shot
        notifyUser(String.valueOf(userId),
                new NotificationPayload("ALL_READ", null, null, null, null, Instant.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void saveAndPush(Long userId, NotificationPayload payload) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(wrapI18n(payload.titleKey(), payload.title()))
                .message(wrapI18n(payload.titleKey(), payload.message(), payload.messageParams()))
                .type(parseType(payload.type()))
                .relatedId(payload.bookingId() != null ? String.valueOf(payload.bookingId()) : null)
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);
        
        // Push real-time with the actual persistence ID and current state
        NotificationPayload pushPayload = new NotificationPayload(
                notification.getId(),
                payload.type(),
                payload.title(),
                payload.message(),
                payload.bookingId(),
                payload.status(),
                notification.isRead(),
                notification.isRead() ? "READ" : "UNREAD",
                notification.getCreatedAt(),
                payload.titleKey(),
                payload.messageParams()
        );
        
        this.notifyUser(String.valueOf(userId), pushPayload);
    }

    @Override
    @Transactional
    public void saveForAdmins(NotificationPayload payload) {
        // Fetch all Staff and Admin users to persist the notification for each
        var admins = userAccountRepository.findAllByRoleNames(java.util.List.of("ADMIN", "STAFF"));
        
        log.info("Saving persistent notifications for {} admins/staff | type={}", admins.size(), payload.type());
        
        var notifications = admins.stream()
                .map(admin -> Notification.builder()
                        .user(admin)
                        .title(wrapI18n(payload.titleKey(), payload.title()))
                        .message(wrapI18n(payload.titleKey(), payload.message(), payload.messageParams()))
                        .type(parseType(payload.type()))
                        .relatedId(payload.bookingId() != null ? String.valueOf(payload.bookingId()) : null)
                        .isRead(false)
                        .build())
                .toList();
        
        notificationRepository.saveAll(notifications);
        
        // Also broadcast via WebSocket for immediate UI refresh
        this.sendToTopic("/topic/admin/bookings", payload);
    }

    private String wrapI18n(String key, String fallback, Object... params) {
        if (key == null) return fallback;
        try {
            return "I18N:" + objectMapper.writeValueAsString(new I18nData(key, params));
        } catch (Exception e) {
            return fallback;
        }
    }

    private NotificationType parseType(String type) {
        try {
            return NotificationType.valueOf(type);
        } catch (Exception e) {
            log.warn("Unknown notification type: {}. Defaulting to BOOKING_STATUS", type);
            return NotificationType.BOOKING_STATUS;
        }
    }

    @Override
    @Transactional
    public void delete(Long notificationId, Long userId) {
        notificationRepository.deleteByIdAndUserId(notificationId, userId);
    }

    @Override
    @Transactional
    public void deleteMultiple(java.util.List<Long> ids, Long userId) {
        notificationRepository.deleteAllByIdInAndUserId(ids, userId);
    }

    @Override
    @Transactional
    public void clearAll(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    private record I18nData(String key, Object[] params) {}
}

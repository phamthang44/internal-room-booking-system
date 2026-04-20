package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.model.NotificationPayload;
import com.thang.roombooking.common.enums.NotificationType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.entity.Notification;
import com.thang.roombooking.entity.UserAccount;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    public void notifyUser(String userId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                new NotificationPayload(type, message, null, null, null, null)
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
    public Page<Notification> getNotificationsByUser(Long userId, Pageable pageable) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            log.warn("Notification {} not found or not owned by user {}", notificationId, userId);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
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
                .title(payload.title())
                .message(payload.message())
                .type(parseType(payload.type()))
                .relatedId(payload.bookingId() != null ? String.valueOf(payload.bookingId()) : null)
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);
        
        // Push real-time
        this.notifyUser(String.valueOf(userId), payload);
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
                        .title(payload.title())
                        .message(payload.message())
                        .type(parseType(payload.type()))
                        .relatedId(payload.bookingId() != null ? String.valueOf(payload.bookingId()) : null)
                        .isRead(false)
                        .build())
                .toList();
        
        notificationRepository.saveAll(notifications);
        
        // Also broadcast via WebSocket for immediate UI refresh
        this.sendToTopic("/topic/admin/bookings", payload);
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
}

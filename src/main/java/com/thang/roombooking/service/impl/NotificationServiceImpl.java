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
                .type(NotificationType.BOOKING_STATUS) // Mapping can be dynamic later
                .relatedId(String.valueOf(payload.bookingId()))
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        // Push real-time
        this.notifyUser(String.valueOf(userId), payload);
    }
}

package com.thang.roombooking.infrastructure.listener;

import com.thang.roombooking.common.dto.request.InAppNotificationRequest;
import com.thang.roombooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${roombooking.rabbitmq.queues.notification-in-app}")
    public void handleInAppNotification(InAppNotificationRequest request) {
        log.info("Received in-app notification for user {} | type={}", 
                request.getUserId(), request.getPayload().type());
        
        try {
            notificationService.saveAndPush(request.getUserId(), request.getPayload());
        } catch (Exception e) {
            log.error("Error processing in-app notification: {}", e.getMessage(), e);
            // Optionally: handle retry or dead-letter queue
        }
    }
}

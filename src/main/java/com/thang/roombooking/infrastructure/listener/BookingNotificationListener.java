package com.thang.roombooking.infrastructure.listener;

import com.thang.roombooking.common.dto.model.NotificationPayload;
import com.thang.roombooking.common.dto.request.InAppNotificationRequest;
import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.infrastructure.configuration.RoomBookingRabbitMQProperties;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationListener {

    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;
    private final RoomBookingRabbitMQProperties rabbitMQProperties;

    @EventListener
    @Async
    public void handleBookingStatusChanged(BookingStatusChangedEvent event) {
        log.info("Received BookingStatusChangedEvent | id={} | status={}", 
                event.booking().getId(), event.statusAfter());

        NotificationPayload payload = buildPayload(event);

        // 1. Notify the Student (Booking Owner) via RabbitMQ (for Persistence + WebSocket)
        Long studentId = event.booking().getUser().getId();
        InAppNotificationRequest request = new InAppNotificationRequest(studentId, payload);
        
        String routingKey = "notification.in-app.booking"; 
        rabbitTemplate.convertAndSend(rabbitMQProperties.getExchange(), routingKey, request);

        // 2. Notify Admins/Staff (Direct WebSocket topic for instant dashboard refresh)
        notificationService.sendToTopic("/topic/admin/bookings", payload);
    }

    private NotificationPayload buildPayload(BookingStatusChangedEvent event) {
        // Mapping BookingStatus to the keys in messages.properties
        String keyPrefix = switch (event.statusAfter()) {
            case PENDING -> "created";
            case CHECKED_IN -> "checkin";
            case COMPLETED -> "checkout";
            default -> event.statusAfter().name().toLowerCase();
        };
        
        String type = "BOOKING_" + event.statusAfter().name();
        String title = I18nUtils.get("notification.booking." + keyPrefix + ".title");
        
        // Match the placeholder {0} in messages.properties which expects Room Number
        String message = I18nUtils.get("notification.booking." + keyPrefix + ".message", 
                event.booking().getClassroom().getRoomName());

        return new NotificationPayload(
                type,
                title,
                message,
                event.booking().getId(),
                event.statusAfter().name(),
                Instant.now()
        );
    }
}

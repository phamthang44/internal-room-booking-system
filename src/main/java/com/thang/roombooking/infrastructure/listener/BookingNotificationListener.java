package com.thang.roombooking.infrastructure.listener;

import com.thang.roombooking.common.dto.model.NotificationPayload;
import com.thang.roombooking.common.dto.request.InAppNotificationRequest;
import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.infrastructure.configuration.RoomBookingRabbitMQProperties;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationListener {

    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;
    private final RoomBookingRabbitMQProperties rabbitMQProperties;
    private final BookingRepository bookingRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional
    public void handleBookingStatusChanged(BookingStatusChangedEvent event) {
        log.info("Received BookingStatusChangedEvent | id={} | status={}", 
                event.booking().getId(), event.statusAfter());

        try {
            // Re-fetch the booking to ensure all lazy collections (classroom, building) are loaded 
            // in this async thread's transaction context. This prevents "statement closed" errors.
            Booking booking = bookingRepository.findByIdWithAdminDetail(event.booking().getId())
                    .orElse(event.booking());

            // 1. Notify the Student (Booking Owner) via RabbitMQ (for Persistence + WebSocket)
            Long studentId = booking.getUser().getId();
            NotificationPayload studentPayload = buildPayload(booking, event, false);
            InAppNotificationRequest request = new InAppNotificationRequest(studentId, studentPayload);
            
            String routingKey = "notification.in-app.booking"; 
            rabbitTemplate.convertAndSend(rabbitMQProperties.getExchange(), routingKey, request);
            log.debug("Pushed notification to status queue for user {}", studentId);

            // 2. Notify Admins/Staff (Save to DB for tracking + WebSocket Broadcast)
            NotificationPayload adminPayload = buildPayload(booking, event, true);
            notificationService.saveForAdmins(adminPayload);
            log.debug("Broadcasted notification to admin topic");

        } catch (Exception e) {
            log.error("Failed to process booking notification: {}", e.getMessage(), e);
        }
    }

    private NotificationPayload buildPayload(Booking booking, BookingStatusChangedEvent event, boolean isAdmin) {
        // Set locale for the current async thread from the event's locale string
        Locale locale = Locale.getDefault();
        if (StringUtils.hasText(event.locale())) {
            try {
                locale = Locale.forLanguageTag(event.locale().replace("_", "-"));
            } catch (Exception e) {
                log.warn("Failed to parse event locale: {}, using default", event.locale());
            }
        }

        // Mapping BookingStatus to the keys in messages.properties
        String keyPrefix = switch (event.statusAfter()) {
            case PENDING -> "created";
            case APPROVED -> "approved";
            case REJECTED -> "rejected";
            case CANCELLED -> "cancelled";
            case CHECKED_IN -> "checkin";
            case COMPLETED -> "checkout";
            default -> "created";
        };
        
        String type = "BOOKING_" + event.statusAfter().name();
        String i18nPrefix = isAdmin ? "notification.admin.booking." : "notification.booking.";
        
        Object[] params;
        if (isAdmin) {
            // Admin message expects: {0}=Name, {1}=Code, {2}=ID, {3}=Room, {4}=PerformedBy, {5}=Note
            params = new Object[]{
                    booking.getUser().getFullName(),                   // {0}
                    booking.getUser().getStudentCode(),                 // {1}
                    booking.getId(),                                    // {2}
                    booking.getClassroom().getRoomName(),               // {3}
                    event.performedBy() != null ? event.performedBy() : "System", // {4}
                    event.note() != null ? event.note() : ""            // {5}
            };
        } else {
            // Student message expects: {0}=Room Name
            params = new Object[]{booking.getClassroom().getRoomName()};
        }

        String title = I18nUtils.get(i18nPrefix + keyPrefix + ".title", locale);
        String message = I18nUtils.get(i18nPrefix + keyPrefix + ".message", locale, params);

        return new NotificationPayload(
                type,
                title,
                message,
                booking.getId(),
                event.statusAfter().name(),
                Instant.now(),
                i18nPrefix + keyPrefix, // titleKey and messageKey share the same prefix
                params
        );
    }
}

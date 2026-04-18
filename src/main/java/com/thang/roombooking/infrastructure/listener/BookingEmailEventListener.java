package com.thang.roombooking.infrastructure.listener;

import com.thang.roombooking.common.dto.model.NotificationPayload;
import com.thang.roombooking.common.enums.BookingAction;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.event.BookingNotificationRequestedEvent;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.NotificationService;
import com.thang.roombooking.service.notification.BookingEmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEmailEventListener {

    private final BookingEmailNotifier bookingEmailNotifier;
    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;

    @RabbitListener(queues = "${roombooking.rabbitmq.queues.email-booking}")
    @Transactional(readOnly = true)
    public void onBookingNotificationRequested(BookingNotificationRequestedEvent event) {
        if (event == null || event.bookingId() == null) return;

        BookingAction action;
        try {
            action = event.action() != null ? BookingAction.valueOf(event.action()) : null;
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown booking action '{}'", event.action());
            return;
        }

        if (action == null) return;

        Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);
        if (booking == null) {
            log.warn("Booking not found for bookingId={}, skip notification", event.bookingId());
            return;
        }

        String roomName = booking.getClassroom() != null ? booking.getClassroom().getRoomName() : "N/A";
        Locale locale = StringUtils.hasText(event.locale()) ? StringUtils.parseLocaleString(event.locale()) : Locale.getDefault();

        switch (action) {
            case CREATE_BOOKING -> {
                bookingEmailNotifier.bookingCreatedPending(booking);
                sendBookingNotification(booking, "BOOKING_CREATED",
                        I18nUtils.get("notification.booking.created.title", locale),
                        I18nUtils.get("notification.booking.created.message", locale, roomName),
                        BookingStatus.PENDING);
            }
            case APPROVE_BOOKING -> {
                bookingEmailNotifier.bookingStatusChanged(booking, event.statusAfter());
                sendBookingNotification(booking, "BOOKING_APPROVED",
                        I18nUtils.get("notification.booking.approved.title", locale),
                        I18nUtils.get("notification.booking.approved.message", locale, roomName),
                        BookingStatus.APPROVED);
            }
            case REJECT_BOOKING, SYSTEM_REJECT -> {
                bookingEmailNotifier.bookingStatusChanged(booking, event.statusAfter());
                sendBookingNotification(booking, "BOOKING_REJECTED",
                        I18nUtils.get("notification.booking.rejected.title", locale),
                        I18nUtils.get("notification.booking.rejected.message", locale, roomName),
                        BookingStatus.REJECTED);
            }
            case CANCEL_BOOKING -> {
                bookingEmailNotifier.bookingCancelled(booking);
                sendBookingNotification(booking, "BOOKING_CANCELLED",
                        I18nUtils.get("notification.booking.cancelled.title", locale),
                        I18nUtils.get("notification.booking.cancelled.message", locale, roomName),
                        BookingStatus.CANCELLED);
            }
            case CHECK_IN -> {
                sendBookingNotification(booking, "BOOKING_CHECKED_IN",
                        I18nUtils.get("notification.booking.checkin.title", locale),
                        I18nUtils.get("notification.booking.checkin.message", locale, roomName),
                        BookingStatus.CHECKED_IN);
            }
            case CHECK_OUT -> {
                sendBookingNotification(booking, "BOOKING_COMPLETED",
                        I18nUtils.get("notification.booking.checkout.title", locale),
                        I18nUtils.get("notification.booking.checkout.message", locale, roomName),
                        BookingStatus.COMPLETED);
            }
            default -> {
                // ignore other actions for now
            }
        }
    }

    private void sendBookingNotification(Booking booking, String type, String title,
                                         String message, BookingStatus status) {
        try {
            String userId = booking.getUser().getId().toString();
            NotificationPayload payload = new NotificationPayload(
                    type,
                    title,
                    message,
                    booking.getId(),
                    status.name(),
                    Instant.now()
            );
            notificationService.notifyUser(userId, payload);
        } catch (Exception e) {
            // WebSocket delivery is best-effort; never fail the email pipeline
            log.warn("Failed to send WS notification for booking {}: {}", booking.getId(), e.getMessage());
        }
    }
}

package com.thang.roombooking.infrastructure.listener;

import com.thang.roombooking.common.enums.BookingAction;
import com.thang.roombooking.common.event.BookingNotificationRequestedEvent;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.notification.BookingEmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEmailEventListener {

    private final BookingEmailNotifier bookingEmailNotifier;
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
            log.warn("Booking not found for bookingId={}, skip email", event.bookingId());
            return;
        }

        switch (action) {
            case CREATE_BOOKING -> bookingEmailNotifier.bookingCreatedPending(booking);
            case APPROVE_BOOKING, REJECT_BOOKING, SYSTEM_REJECT ->
                    bookingEmailNotifier.bookingStatusChanged(booking, event.statusAfter());
            case CANCEL_BOOKING -> bookingEmailNotifier.bookingCancelled(booking);
            default -> {
                // ignore other actions for now
            }
        }
    }
}


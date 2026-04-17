package com.thang.roombooking.infrastructure.listener;

import com.thang.roombooking.common.enums.BookingAction;
import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.service.notification.BookingEmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEmailEventListener {

    private final BookingEmailNotifier bookingEmailNotifier;

    @EventListener
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {
        if (event == null || event.booking() == null) return;

        BookingAction action;
        try {
            action = event.action() != null ? BookingAction.valueOf(event.action()) : null;
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown booking action '{}'", event.action());
            return;
        }

        if (action == null) return;

        switch (action) {
            case CREATE_BOOKING -> bookingEmailNotifier.bookingCreatedPending(event.booking());
            case APPROVE_BOOKING, REJECT_BOOKING, SYSTEM_REJECT ->
                    bookingEmailNotifier.bookingStatusChanged(event.booking(), event.statusAfter());
            case CANCEL_BOOKING -> bookingEmailNotifier.bookingCancelled(event.booking());
            default -> {
                // ignore other actions for now
            }
        }
    }
}


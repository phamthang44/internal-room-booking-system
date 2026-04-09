package com.thang.roombooking.service.listener;

import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.service.BookingHistoryCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingHistoryListener {

    private final BookingHistoryCommandService bookingHistoryCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void logHistoryBooking(BookingStatusChangedEvent event) {
        bookingHistoryCommandService.saveBookingHistory(
                event.booking(),
                event.action(),
                event.performedBy(),
                event.note(),
                event.statusAfter());
    }

}

package com.thang.roombooking.service.listener;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.fixture.BookingFixtures;
import com.thang.roombooking.service.BookingHistoryCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingHistoryListenerTest {

    @Mock private BookingHistoryCommandService bookingHistoryCommandService;

    @InjectMocks
    private BookingHistoryListener listener;

    @Test
    void should_delegate_to_bookingHistoryCommandService_with_correct_args() {
        Booking booking = BookingFixtures.approvedBooking();
        BookingStatusChangedEvent event = new BookingStatusChangedEvent(
                booking,
                BookingStatus.APPROVED,
                "APPROVE_BOOKING",
                "admin@uni.edu.vn",
                "booking.approve.reason.staff",
                "vi"
        );

        listener.logHistoryBooking(event);

        verify(bookingHistoryCommandService).saveBookingHistory(
                booking,
                "APPROVE_BOOKING",
                "admin@uni.edu.vn",
                "booking.approve.reason.staff",
                BookingStatus.APPROVED
        );
    }

    @Test
    void should_handle_system_cancel_event() {
        Booking booking = BookingFixtures.cancelledBooking();
        BookingStatusChangedEvent event = new BookingStatusChangedEvent(
                booking,
                BookingStatus.CANCELLED,
                "CANCEL_BOOKING",
                "SYSTEM",
                "booking.cancel.reason.no_show",
                "vi"
        );

        listener.logHistoryBooking(event);

        verify(bookingHistoryCommandService).saveBookingHistory(
                booking,
                "CANCEL_BOOKING",
                "SYSTEM",
                "booking.cancel.reason.no_show",
                BookingStatus.CANCELLED
        );
    }
}

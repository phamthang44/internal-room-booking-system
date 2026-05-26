package com.thang.roombooking.infrastructure.scheduler;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.fixture.BookingFixtures;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.BookingCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoRejectPendingBookingJobTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingCommandService bookingCommandService;

    @InjectMocks
    private AutoRejectPendingBookingJob job;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "appTimeZone", "Asia/Ho_Chi_Minh");
    }

    @Test
    void should_auto_reject_each_overdue_pending_booking() {
        Booking b1 = BookingFixtures.pendingBooking();
        b1.setId(1L);
        Booking b2 = BookingFixtures.pendingBooking();
        b2.setId(2L);

        when(bookingRepository.findPendingBookingsToAutoReject(
                eq(BookingStatus.PENDING), any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(List.of(b1, b2));

        job.autoRejectBooking();

        verify(bookingCommandService).autoRejectOverduePendingBooking(b1);
        verify(bookingCommandService).autoRejectOverduePendingBooking(b2);
    }

    @Test
    void should_do_nothing_when_no_overdue_pending_bookings() {
        when(bookingRepository.findPendingBookingsToAutoReject(any(), any(), any()))
                .thenReturn(List.of());

        job.autoRejectBooking();

        verify(bookingCommandService, never()).autoRejectOverduePendingBooking(any());
    }

    @Test
    void should_continue_processing_remaining_bookings_when_one_reject_fails() {
        Booking b1 = BookingFixtures.pendingBooking();
        b1.setId(1L);
        Booking b2 = BookingFixtures.pendingBooking();
        b2.setId(2L);

        when(bookingRepository.findPendingBookingsToAutoReject(any(), any(), any()))
                .thenReturn(List.of(b1, b2));
        doThrow(new RuntimeException("Lock timeout")).when(bookingCommandService)
                .autoRejectOverduePendingBooking(b1);

        job.autoRejectBooking();

        verify(bookingCommandService).autoRejectOverduePendingBooking(b1);
        verify(bookingCommandService).autoRejectOverduePendingBooking(b2);
    }
}

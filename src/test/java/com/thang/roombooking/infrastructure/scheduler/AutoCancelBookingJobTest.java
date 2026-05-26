package com.thang.roombooking.infrastructure.scheduler;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingTimeSlot;
import com.thang.roombooking.entity.TimeSlot;
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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoCancelBookingJobTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingCommandService bookingCommandService;

    @InjectMocks
    private AutoCancelBookingJob job;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "appTimeZone", "Asia/Ho_Chi_Minh");
    }

    @Test
    void should_cancel_bookings_past_no_show_window() {
        // Booking started 20 minutes ago (past the 15-min no-show window)
        LocalTime pastStartTime = LocalTime.now().minusMinutes(20);
        Booking expiredBooking = buildApprovedBookingWithStartTime(pastStartTime);

        when(bookingRepository.findApprovedBookingsForAutoCancel(eq(BookingStatus.APPROVED), any(LocalDate.class)))
                .thenReturn(List.of(expiredBooking));

        job.autoCancelBooking();

        verify(bookingCommandService).cancelExpiredBooking(expiredBooking);
    }

    @Test
    void should_not_cancel_bookings_within_no_show_window() {
        // Booking starting in 5 minutes (still within window)
        LocalTime futureStartTime = LocalTime.now().plusMinutes(5);
        Booking futureBooking = buildApprovedBookingWithStartTime(futureStartTime);

        when(bookingRepository.findApprovedBookingsForAutoCancel(eq(BookingStatus.APPROVED), any(LocalDate.class)))
                .thenReturn(List.of(futureBooking));

        job.autoCancelBooking();

        verify(bookingCommandService, never()).cancelExpiredBooking(any());
    }

    @Test
    void should_not_call_repository_cancel_when_no_approved_bookings() {
        when(bookingRepository.findApprovedBookingsForAutoCancel(any(), any())).thenReturn(List.of());

        job.autoCancelBooking();

        verify(bookingCommandService, never()).cancelExpiredBooking(any());
    }

    @Test
    void should_continue_processing_other_bookings_when_one_cancel_fails() {
        LocalTime pastStartTime = LocalTime.now().minusMinutes(20);
        Booking booking1 = buildApprovedBookingWithStartTime(pastStartTime);
        booking1.setId(1L);
        Booking booking2 = buildApprovedBookingWithStartTime(pastStartTime);
        booking2.setId(2L);

        when(bookingRepository.findApprovedBookingsForAutoCancel(any(), any()))
                .thenReturn(List.of(booking1, booking2));
        doThrow(new RuntimeException("DB error")).when(bookingCommandService).cancelExpiredBooking(booking1);

        job.autoCancelBooking();

        verify(bookingCommandService).cancelExpiredBooking(booking1);
        verify(bookingCommandService).cancelExpiredBooking(booking2);
    }

    private Booking buildApprovedBookingWithStartTime(LocalTime startTime) {
        TimeSlot timeSlot = TimeSlot.builder()
                .id(1).startTime(startTime).endTime(startTime.plusHours(2))
                .build();
        BookingTimeSlot bts = BookingTimeSlot.builder().timeSlot(timeSlot).build();

        Booking booking = BookingFixtures.approvedBooking();
        booking.setBookingDate(LocalDate.now());
        booking.setBookingTimeSlots(new ArrayList<>(List.of(bts)));
        return booking;
    }
}

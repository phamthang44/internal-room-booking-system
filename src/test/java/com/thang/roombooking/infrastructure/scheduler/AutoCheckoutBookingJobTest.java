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
class AutoCheckoutBookingJobTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingCommandService bookingCommandService;

    @InjectMocks
    private AutoCheckoutBookingJob job;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "appTimeZone", "Asia/Ho_Chi_Minh");
    }

    @Test
    void should_auto_checkout_each_expired_checked_in_booking() {
        Booking b1 = BookingFixtures.checkedInBooking();
        b1.setId(1L);
        Booking b2 = BookingFixtures.checkedInBooking();
        b2.setId(2L);

        when(bookingRepository.findCheckedInBookingsToAutoCheckout(
                eq(BookingStatus.CHECKED_IN), any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(List.of(b1, b2));

        job.autoCheckoutBooking();

        verify(bookingCommandService).autoCheckoutExpiredBooking(b1);
        verify(bookingCommandService).autoCheckoutExpiredBooking(b2);
    }

    @Test
    void should_do_nothing_when_no_bookings_need_auto_checkout() {
        when(bookingRepository.findCheckedInBookingsToAutoCheckout(any(), any(), any()))
                .thenReturn(List.of());

        job.autoCheckoutBooking();

        verify(bookingCommandService, never()).autoCheckoutExpiredBooking(any());
    }
}

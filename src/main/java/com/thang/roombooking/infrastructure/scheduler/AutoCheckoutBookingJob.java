package com.thang.roombooking.infrastructure.scheduler;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.BookingCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCheckoutBookingJob {

    private final BookingRepository bookingRepository;
    private final BookingCommandService bookingCommandService;

    /**
     * Auto-checkout CHECKED_IN bookings once their end time has passed.
     * Runs every minute to keep availability accurate.
     */
    @Scheduled(cron = "15 * * * * *") // every minute at second 15
    public void autoCheckoutBooking() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalTime now = LocalTime.now(ZoneOffset.UTC);

        List<Booking> toCheckout = bookingRepository.findCheckedInBookingsToAutoCheckout(
                BookingStatus.CHECKED_IN,
                today,
                now
        );

        if (toCheckout.isEmpty()) {
            return;
        }

        log.info("{} | Auto-checkout {} booking(s)", LogConstant.ACTION_START, toCheckout.size());
        for (Booking b : toCheckout) {
            bookingCommandService.autoCheckoutExpiredBooking(b);
        }
    }
}


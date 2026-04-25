package com.thang.roombooking.infrastructure.scheduler;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingTimeSlot;
import com.thang.roombooking.entity.TimeSlot;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.BookingCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCancelBookingJob {

    private final BookingRepository bookingRepository;
    private final BookingCommandService bookingCommandService;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String appTimeZone;

    @Scheduled(cron = "0 */5 * * * *")
    public void autoCancelBooking() {
        log.info("{} | Scan approved bookings past the no-show window", LogConstant.ACTION_START);

        ZoneId appZone = ZoneId.of(appTimeZone);
        ZonedDateTime now = ZonedDateTime.now(appZone);
        LocalDate today = now.toLocalDate();

        List<Booking> expiredBookings = bookingRepository.findApprovedBookingsForAutoCancel(BookingStatus.APPROVED, today)
                .stream()
                .filter(booking -> isPastNoShowWindow(booking, now, appZone))
                .toList();

        if (expiredBookings.isEmpty()) {
            return;
        }

        for (Booking booking : expiredBookings) {
            try {
                bookingCommandService.cancelExpiredBooking(booking);
            } catch (Exception e) {
                log.error("Auto-cancel failed for booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }

    private boolean isPastNoShowWindow(Booking booking, ZonedDateTime now, ZoneId appZone) {
        LocalTime earliestStartTime = booking.getBookingTimeSlots().stream()
                .map(BookingTimeSlot::getTimeSlot)
                .map(TimeSlot::getStartTime)
                .min(LocalTime::compareTo)
                .orElse(booking.getStartTime());
        //26-04-2026 start : 7:00
        //min là lấy ra giá trị min nhất trong 2 time slot nó có ví dụ 7:00 9:00 9:30 11:30
        //thì lấy 7:00 ko thì lấy thẳng logic trong getStartTime() vì đoạn này đã đc xử lí khâu tạo booking
        if (booking.getBookingDate() == null || earliestStartTime == null) {
            log.warn("Skipping auto-cancel for booking {} because bookingDate/startTime is missing", booking.getId());
            return false;
        }
        //time cancelAt thì lấy date.atTime(7:00) + 15p return nếu now đứng sau time autoCancelAt ở đây là 7:15
        ZonedDateTime autoCancelAt = booking.getBookingDate()
                .atTime(earliestStartTime)
                .atZone(appZone)
                .plusMinutes(15);

        return now.isAfter(autoCancelAt);
    }
}

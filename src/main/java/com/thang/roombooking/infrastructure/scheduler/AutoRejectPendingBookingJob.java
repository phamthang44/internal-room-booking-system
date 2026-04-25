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

import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoRejectPendingBookingJob {

    private final BookingRepository bookingRepository;
    private final BookingCommandService bookingCommandService;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String appTimeZone;

    @Scheduled(cron = "0 * * * * *") // Chạy mỗi phút
    public void autoRejectBooking() {
        log.info("{} | Quét đơn chưa duyệt quá hạn (PENDING -> REJECTED)", LogConstant.ACTION_START);

        // Ngưỡng thời gian: Hủy khi đơn PENDING đã tới giờ bắt đầu booking (overtime)
        // Dùng UTC đồng bộ với DB
        LocalTime thresholdTime = LocalTime.now(ZoneId.of(appTimeZone));
        LocalDate today = LocalDate.now(ZoneId.of(appTimeZone));

        List<Booking> expiredBookings = bookingRepository.findPendingBookingsToAutoReject(
                BookingStatus.PENDING,
                today,
                thresholdTime
        );

        if (expiredBookings.isEmpty()) {
            return;
        }

        for (Booking booking : expiredBookings) {
            try {
                bookingCommandService.autoRejectOverduePendingBooking(booking);
                log.info("{} | Đã tự động từ chối đơn ID {} vì quá tới hạn giờ xử lý.", LogConstant.ACTION_SUCCESS, booking.getId());
            } catch (Exception e) {
                log.error("{} | Lỗi auto-reject PENDING đơn ID {}: {}", LogConstant.SYS_ERROR, booking.getId(), e.getMessage());
            }
        }
    }
}

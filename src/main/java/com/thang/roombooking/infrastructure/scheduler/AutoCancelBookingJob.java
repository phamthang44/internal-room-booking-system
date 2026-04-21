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
public class AutoCancelBookingJob {

    private final BookingRepository bookingRepository;
    private final BookingCommandService bookingCommandService;

    @Scheduled(cron = "0 */5 * * * *") // Chạy mỗi 5 phút/lần cho nhẹ máy
    public void autoCancelBooking() {
        log.info("{} | Quét đơn quá hạn check-in", LogConstant.ACTION_START);

        // Ngưỡng thời gian: Hiện tại (UTC) - 15 phút
        // Ví dụ: Bây giờ 8:16 -> Tìm các đơn có Slot bắt đầu trước 8:01 mà chưa check-in
        LocalTime thresholdTime = LocalTime.now(ZoneOffset.UTC).minusMinutes(15);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(
                BookingStatus.APPROVED,
                today,
                thresholdTime
        );

        if (expiredBookings.isEmpty()) {
            return;
        }

        for (Booking booking : expiredBookings) {
            try {
                // Gọi sang Service xử lý Atomic Update để an toàn concurrency
                bookingCommandService.cancelExpiredBooking(booking);
            } catch (Exception e) {
                log.error("Lỗi auto-cancel ID {}: {}", booking.getId(), e.getMessage());
            }
        }
    }
}

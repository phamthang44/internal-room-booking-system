package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.policy.BookingPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;

import static com.thang.roombooking.common.constant.SystemConstant.SYSTEM_REGION_TIMEZONE;
import static com.thang.roombooking.common.constant.TimeConstant.CLOSING_TIME;
import static com.thang.roombooking.common.constant.TimeConstant.OPENING_TIME;

@Component
@RequiredArgsConstructor
public class BookingPolicyImpl implements BookingPolicy {

    private final BookingRepository bookingRepository;

    @Override
    public void validateLeadTimePolicy(LocalDate date) {
        if (date.isAfter(LocalDate.now(ZoneId.of(SYSTEM_REGION_TIMEZONE)).plusDays(7))) {
            throw new AppException(BookingErrorCode.BOOKING_DATE_OVER_LIMIT);
        }
    }

    @Override
    public void validateQuotaPolicy(Long userId, LocalDate date, int requestedSlots) {
        // Count number of reserved time-slots (not number of bookings).
        // This prevents users from bypassing quota by creating bookings in other rooms.
        long bookedTodaySlots = bookingRepository.countBookedSlotsByUserAndDateAndStatuses(
                userId,
                date,
                List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED) // active bookings
        );

        int maxQuota = 2;
        // Quy tắc: tối đa 2 slots (tương đương 4 tiếng) mỗi ngày
        if (bookedTodaySlots + requestedSlots > maxQuota) {
            throw new AppException(BookingErrorCode.BOOKING_QUOTA_EXCEEDED, maxQuota, 1);
        }
    }

    @Override
    public void validatePenalty(Long userId) {

    }

    @Override
    public void validateBookingTimeWorkingHours(LocalDate bookingDate, Instant bookingTime) {
        // Chỉ validate giờ làm việc nếu là đặt cho ngày hôm nay
        // Nếu đặt cho tương lai (ngày mai trở đi), bỏ qua check này
        if (!bookingDate.equals(LocalDate.now(ZoneId.of(SYSTEM_REGION_TIMEZONE)))) {
            return;
        }

        // Chuyển Instant sang giờ Việt Nam
        LocalTime now = LocalTime.ofInstant(bookingTime, ZoneId.of(SYSTEM_REGION_TIMEZONE));

        if (now.isBefore(OPENING_TIME) || now.isAfter(CLOSING_TIME)) {
            throw new AppException(BookingErrorCode.BOOKING_OUT_OF_WORKING_HOURS,
                    OPENING_TIME, CLOSING_TIME);
        }
    }

    @Override
    public void validateNoOverlappingActiveBookings(Long userId, LocalDate bookingDate, List<Integer> requestedTimeSlotIds) {
        if (requestedTimeSlotIds == null || requestedTimeSlotIds.isEmpty()) return;

        List<Long> conflictingIds = bookingRepository.findConflictingBookingIds(
                userId,
                bookingDate,
                List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.CHECKED_IN),
                requestedTimeSlotIds
        );

        if (conflictingIds != null && !conflictingIds.isEmpty()) {
            throw new AppException(BookingErrorCode.BOOKING_USER_DAILY_SLOT_CONFLICT, conflictingIds.getFirst());
        }
    }


}

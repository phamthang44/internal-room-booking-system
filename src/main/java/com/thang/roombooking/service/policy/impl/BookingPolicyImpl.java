package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.policy.BookingPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;

import static com.thang.roombooking.common.constant.TimeConstant.CLOSING_TIME;
import static com.thang.roombooking.common.constant.TimeConstant.OPENING_TIME;

@Component
@RequiredArgsConstructor
public class BookingPolicyImpl implements BookingPolicy {

    private final BookingRepository bookingRepository;

    @Override
    public void validateLeadTimePolicy(LocalDate date) {
        if (date.isAfter(LocalDate.now().plusDays(7))) {
            throw new AppException(BookingErrorCode.BOOKING_DATE_OVER_LIMIT);
        }
    }

    @Override
    public void validateQuotaPolicy(Long userId, LocalDate date, int requestedSlots) {
        // Đếm số slot User đã đặt trong ngày hôm đó
        // Chỉ tin vào bảng Bookings - Nơi lưu giữ sự thật
        long bookedToday = bookingRepository.countByUserIdAndBookingDateAndStatusIn(
                userId,
                date,
                List.of(BookingStatus.PENDING, BookingStatus.APPROVED) // Chỉ tính đơn còn hiệu lực
        );

        int maxQuota = 2;
        // Quy tắc: tối đa 2 slots (tương đương 4 tiếng) mỗi ngày
        if (bookedToday + requestedSlots > maxQuota) {
            throw new AppException(BookingErrorCode.BOOKING_QUOTA_EXCEEDED);
        }
    }

    @Override
    public void validatePenalty(Long userId) {

    }

    @Override
    public void validateBookingTimeWorkingHours(Instant bookingTime) {
        // Chuyển Instant sang giờ Việt Nam
        LocalTime now = LocalTime.ofInstant(bookingTime, ZoneId.of("Asia/Ho_Chi_Minh"));

//        LocalTime openingTime = LocalTime.of(6, 30); // 6h30
//        LocalTime closingTime = LocalTime.of(17, 30); // 17h30

        if (now.isBefore(OPENING_TIME) || now.isAfter(CLOSING_TIME)) {
            throw new AppException(BookingErrorCode.BOOKING_OUT_OF_WORKING_HOURS,
                    OPENING_TIME, CLOSING_TIME);
        }
    }


}

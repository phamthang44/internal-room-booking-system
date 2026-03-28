package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.service.policy.BookingFlowPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class BookingFlowPolicyImpl implements BookingFlowPolicy {

    @Override
    public void validateCheckInTimePolicy(Instant bookingStartTime) {
        // Chuyển Instant sang Giờ địa phương (Việt Nam) để so sánh với TimeSlot
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalTime startTime = LocalDateTime.ofInstant(bookingStartTime, ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime();

        // 1. Chặn đến quá sớm (Hơn 30 phút)
        if (now.isBefore(startTime.minusMinutes(30))) { //"Vui lòng quay lại sau, cửa phòng chỉ mở trước 30 phút."
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_TOO_EARLY, 30);
        }

        // 2. Chặn đến quá muộn (Quá 15 phút - Dành cho lúc User nhấn nút Check-in)
        if (now.isAfter(startTime.plusMinutes(15))) { //"Đã quá 15 phút nhận phòng, đơn đặt của bạn đã bị hủy."
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_EXPIRED, 15);
        }
    }

    @Override
    public void validateCancelConditionPolicy(Long bookingId) {

    }

    @Override
    public void validatePenaltyPolicy() {

    }
}

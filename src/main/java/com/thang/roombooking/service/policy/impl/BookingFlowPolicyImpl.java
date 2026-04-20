package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.service.policy.BookingFlowPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class BookingFlowPolicyImpl implements BookingFlowPolicy {


    @Override
    public void validateCheckInTimePolicy(Instant bookingStartTime) {
        // IMPORTANT: must compare full date-time, not only LocalTime.
        // Comparing only LocalTime allows early check-in on a different day
        // (e.g. tomorrow 10:00 vs today 10:05 would incorrectly pass).
        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDateTime now = LocalDateTime.now(vnZone);
        LocalDateTime startDateTime = LocalDateTime.ofInstant(bookingStartTime, vnZone);

        // 0. Chặn check-in sai ngày (không phải ngày booking)
        LocalDate today = now.toLocalDate();
        LocalDate bookingDate = startDateTime.toLocalDate();
        if (bookingDate.isAfter(today)) {
            long daysDiff = ChronoUnit.DAYS.between(today, bookingDate);
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_TOO_EARLY_DAYS, daysDiff);
        }
        if (bookingDate.isBefore(today)) {
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_EXPIRED_DAYS);
        }

        // 1. Chặn đến quá sớm (Hơn 30 phút)
        if (now.isBefore(startDateTime.minusMinutes(30))) { //"Vui lòng quay lại sau, cửa phòng chỉ mở trước 30 phút."
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_TOO_EARLY, 30);
        }

        // 2. Chặn đến quá muộn (Quá 15 phút - Dành cho lúc User nhấn nút Check-in)
        if (now.isAfter(startDateTime.plusMinutes(15))) { //"Đã quá 15 phút nhận phòng, đơn đặt của bạn đã bị hủy."
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_EXPIRED, 15);
        }
    }

    @Override
    public void validateCancelConditionPolicy(Instant bookingCreatedAt, BookingStatus bookingStatus, LocalDateTime bookingStartDateTime) {

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        // 1. Kiểm tra trạng thái tĩnh (như bạn đã làm)
        if (bookingStatus == BookingStatus.CANCELLED) {
            throw new AppException(BookingErrorCode.BOOKING_STATUS_ALREADY_CANCELLED);
        }
        if (bookingStatus == BookingStatus.CHECKED_IN) {
            throw new AppException(BookingErrorCode.BOOKING_CANNOT_CANCEL_AFTER_CHECKED_IN);
        }

        // 2. Logic: Nếu đã Approved thì không cho hủy tự động (Phải gọi Staff)
        if (bookingStatus == BookingStatus.APPROVED) {
            throw new AppException(BookingErrorCode.BOOKING_CANNOT_CANCEL_AFTER_APPROVAL);
        }

        // 3. Logic thời gian linh hoạt (Ví dụ: Chỉ được hủy trước giờ bắt đầu 1 tiếng)
        if (now.isAfter(bookingStartDateTime)) {
            throw new AppException(BookingErrorCode.BOOKING_DATE_IN_PAST); // chặn hủy trong quá khứ
        }

        Duration durationUntilStart = Duration.between(now, bookingStartDateTime);
        if (durationUntilStart.toMinutes() < 60) {
            throw new AppException(BookingErrorCode.BOOKING_CANCEL_TOO_LATE);
        }
    }

    @Override
    public void validatePenaltyPolicy() {

    }

    @Override
    public void validateCheckInStatus(BookingStatus bookingStatus) {
        // Case 1: Booking vẫn đang PENDING hoặc bị REJECTED
        if (bookingStatus != BookingStatus.APPROVED && bookingStatus != BookingStatus.CHECKED_IN && bookingStatus != BookingStatus.COMPLETED) {
            throw new AppException(BookingErrorCode.BOOKING_NOT_APPROVED);
        }
        // Case 2: Đã check-in rồi
        if (bookingStatus == BookingStatus.CHECKED_IN || bookingStatus == BookingStatus.COMPLETED) {
            throw new AppException(BookingErrorCode.BOOKING_ALREADY_CHECKED_IN);
        }
    }

    @Override
    public void validateRejectStatus(BookingStatus bookingStatus) {
        if (bookingStatus != BookingStatus.PENDING) {
            throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
        }
    }


    @Override
    public void validateApproveStatus(BookingStatus bookingStatus) {
        // Case 1: khác PENDING
        if (bookingStatus != BookingStatus.PENDING) {
            throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
        }
    }

}

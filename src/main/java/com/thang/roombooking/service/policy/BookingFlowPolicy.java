package com.thang.roombooking.service.policy;

import com.thang.roombooking.common.enums.BookingStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public interface BookingFlowPolicy {

    void validateCheckInTimePolicy(Instant bookingTime); // check 15 phút đầu + là ca đầu tiên trong ngày 7h sáng

    void validateCheckOutTimePolicy(Instant bookingStartTime);

    void validateCancelConditionPolicy(Instant bookingCreatedAt, BookingStatus bookingStatus, LocalDateTime bookingStartDateTime);

    void validatePenaltyPolicy();

    void validateCheckInStatus(BookingStatus bookingStatus);

    void validateApproveStatus(BookingStatus bookingStatus);

    void validateRejectStatus(BookingStatus bookingStatus);
}

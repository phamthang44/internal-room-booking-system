package com.thang.roombooking.service.policy;

import java.time.Instant;

public interface BookingFlowPolicy {

    void validateCheckInTimePolicy(Instant bookingTime); // check 15 phút đầu + là ca đầu tiên trong ngày 7h sáng

    void validateCancelConditionPolicy(Long bookingId); //check hủy phải đc thực hiện trước 30p sau khi đặt

    void validatePenaltyPolicy();

}

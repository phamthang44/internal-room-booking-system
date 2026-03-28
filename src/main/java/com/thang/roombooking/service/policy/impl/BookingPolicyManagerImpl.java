package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.service.policy.BookingFlowPolicy;
import com.thang.roombooking.service.policy.BookingPolicy;
import com.thang.roombooking.service.policy.BookingPolicyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingPolicyManagerImpl implements BookingPolicyManager {

    private final BookingPolicy bookingPolicy;
    private final BookingFlowPolicy bookingFlowPolicy;


    @Override
    public void validateLeadTimePolicy(LocalDate date) {
        bookingPolicy.validateLeadTimePolicy(date);
    }

    @Override
    public void validateQuotaPolicy(Long userId, LocalDate date, int requestedSlots) {
        bookingPolicy.validateQuotaPolicy(userId, date, requestedSlots);
    }

    @Override
    public void validatePenalty(Long userId) {
        // TODO tạm thời bỏ qua tính năng policy này
    }

    @Override
    public void validateCheckInTimePolicy(Instant bookingStartTime) {

    }

    @Override
    public void validateBookingTimeWorkingHours(Instant bookingTime) {
        bookingPolicy.validateBookingTimeWorkingHours(bookingTime);
    }

    @Override
    public void validateCancelConditionPolicy(Long bookingId) {

    }

    @Override
    public void validatePenaltyPolicy() {

    }
}

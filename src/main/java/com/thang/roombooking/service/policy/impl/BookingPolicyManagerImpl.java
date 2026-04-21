package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.service.policy.BookingFlowPolicy;
import com.thang.roombooking.service.policy.BookingPolicy;
import com.thang.roombooking.service.policy.BookingPolicyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    public void validateNoOverlappingActiveBookings(Long userId, LocalDate bookingDate, List<Integer> requestedTimeSlotIds) {
        bookingPolicy.validateNoOverlappingActiveBookings(userId, bookingDate, requestedTimeSlotIds);
    }

    @Override
    public void validateCheckInTimePolicy(Instant bookingStartTime) {
        bookingFlowPolicy.validateCheckInTimePolicy(bookingStartTime);
    }

    @Override
    public void validateCheckOutTimePolicy(Instant bookingStartTime) {
        bookingFlowPolicy.validateCheckOutTimePolicy(bookingStartTime);
    }

    @Override
    public void validateBookingTimeWorkingHours(LocalDate bookingDate, Instant bookingTime) {
        bookingPolicy.validateBookingTimeWorkingHours(bookingDate, bookingTime);
    }

    @Override
    public void validateCheckInStatus(BookingStatus bookingStatus) {
        bookingFlowPolicy.validateCheckInStatus(bookingStatus);
    }

    @Override
    public void validateApproveStatus(BookingStatus bookingStatus) {
        bookingFlowPolicy.validateApproveStatus(bookingStatus);
    }

    @Override
    public void validateRejectStatus(BookingStatus bookingStatus) {
        bookingFlowPolicy.validateRejectStatus(bookingStatus);
    }

    @Override
    public void validateCancelConditionPolicy(Instant bookingCreatedAt, BookingStatus bookingStatus, Instant bookingStartTime) {
        bookingFlowPolicy.validateCancelConditionPolicy(bookingCreatedAt, bookingStatus, bookingStartTime);
    }

    @Override
    public void validatePenaltyPolicy() {

    }
}

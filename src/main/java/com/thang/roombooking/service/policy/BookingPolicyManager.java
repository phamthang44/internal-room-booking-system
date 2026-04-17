package com.thang.roombooking.service.policy;

import com.thang.roombooking.common.enums.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public interface BookingPolicyManager {

    void validateLeadTimePolicy(LocalDate date);

    void validateQuotaPolicy(Long userId, LocalDate date, int requestedSlots);

    void validatePenalty(Long userId);

    void validateNoOverlappingActiveBookings(Long userId, LocalDate bookingDate, List<Integer> requestedTimeSlotIds);

    void validateCheckInTimePolicy(Instant bookingStartTime);

    void validateBookingTimeWorkingHours(LocalDate bookingDate, Instant bookingTime);

    void validateCheckInStatus(BookingStatus bookingStatus);

    void validateApproveStatus(BookingStatus bookingStatus);

    void validateCancelConditionPolicy(Instant bookingCreatedAt, BookingStatus bookingStatus, LocalDateTime bookingStartDateTime);

    void validatePenaltyPolicy();
}

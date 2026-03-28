package com.thang.roombooking.service.policy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface BookingPolicyManager {

    void validateLeadTimePolicy(LocalDate date);

    void validateQuotaPolicy(Long userId, LocalDate date, int requestedSlots);

    void validatePenalty(Long userId);

    void validateCheckInTimePolicy(Instant bookingStartTime);

    void validateBookingTimeWorkingHours(Instant bookingTime);



    void validateCancelConditionPolicy(Long bookingId);

    void validatePenaltyPolicy();
}

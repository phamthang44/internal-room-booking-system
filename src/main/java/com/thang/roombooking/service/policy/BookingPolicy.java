package com.thang.roombooking.service.policy;

import java.time.Instant;
import java.time.LocalDate;

public interface BookingPolicy {
    void validateLeadTimePolicy(LocalDate date); // check 7 ngày

    void validateQuotaPolicy(Long userId, LocalDate date, int requestedSlots); // check 4 tiếng + account user

    void validatePenalty(Long userId); //check blacklist case book xong ko tới check in

    void validateBookingTimeWorkingHours(Instant bookingTime);
}

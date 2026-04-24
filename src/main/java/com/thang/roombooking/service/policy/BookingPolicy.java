package com.thang.roombooking.service.policy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.thang.roombooking.entity.Booking;

public interface BookingPolicy {
    void validateLeadTimePolicy(LocalDate date); // check 7 ngày

    void validateQuotaPolicy(Long userId, LocalDate date, int requestedSlots); // check 4 tiếng + account user

    void validatePenalty(Long userId); //check blacklist case book xong ko tới check in
    
    void validatePendingQuota(Long userId); // check tối đa 3 đơn PENDING

    void validateBookingTimeWorkingHours(LocalDate bookingDate, Instant bookingTime);

    void validateNoOverlappingActiveBookings(Long userId, LocalDate bookingDate, List<Integer> requestedTimeSlotIds);
    
    void checkCancellationSpam(Booking booking);

    void handleNoShowViolation(Booking booking);
}

package com.thang.roombooking.service.notification;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;

/**
 * Use-case focused outbound email port for booking notifications.
 */
public interface BookingEmailNotifier {

    void bookingCreatedPending(Booking booking);

    void bookingStatusChanged(Booking booking, BookingStatus statusAfter);

    void roomAvailabilityUpdated(Long classroomId);

    void concurrentBookingDetected(Booking booking);

    void bookingCancelled(Booking booking);
}


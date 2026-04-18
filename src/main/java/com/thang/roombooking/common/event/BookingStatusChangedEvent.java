package com.thang.roombooking.common.event;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;

public record BookingStatusChangedEvent(
        Booking booking,
        BookingStatus statusAfter,
        String action,
        String performedBy,
        String note,
        String locale
) {}
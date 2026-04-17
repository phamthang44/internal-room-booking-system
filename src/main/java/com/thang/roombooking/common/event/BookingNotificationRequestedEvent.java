package com.thang.roombooking.common.event;

import com.thang.roombooking.common.enums.BookingStatus;

import java.time.Instant;

public record BookingNotificationRequestedEvent(
        String eventId,
        Instant occurredAt,
        Long bookingId,
        String action,
        BookingStatus statusAfter,
        String performedBy,
        String note
) {}


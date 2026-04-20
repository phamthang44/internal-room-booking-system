package com.thang.roombooking.common.dto.model;

import java.time.Instant;

public record NotificationPayload(
        String type,       // e.g. "BOOKING_APPROVED", "BOOKING_CANCELLED"
        String title,      // e.g. "Booking Approved"
        String message,    // e.g. "Your booking for room A101 has been approved."
        Long bookingId,    // For optional click-to-navigate in the frontend
        String status,     // BookingStatus string for frontend styling (green/red/yellow)
        Instant timestamp, // Server timestamp
        String titleKey,   // e.g. "notification.booking.approved.title"
        Object[] messageParams // e.g. ["Room A101", "Student Name"]
) {
    public NotificationPayload(String type, String title, String message, Long bookingId, String status, Instant timestamp) {
        this(type, title, message, bookingId, status, timestamp, null, null);
    }
}

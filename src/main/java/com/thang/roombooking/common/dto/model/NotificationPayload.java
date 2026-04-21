package com.thang.roombooking.common.dto.model;

import com.thang.roombooking.entity.Notification;
import java.time.Instant;

public record NotificationPayload(
        Long id,           // Persistence ID (if saved)
        String type,       // e.g. "BOOKING_APPROVED", "BOOKING_CANCELLED"
        String title,      // e.g. "Booking Approved"
        String message,    // e.g. "Your booking for room A101 has been approved."
        Long bookingId,    // For optional click-to-navigate in the frontend
        String status,     // BookingStatus string for frontend styling (green/red/yellow)
        boolean isRead,    // Read status
        String readStatus, // "READ" or "UNREAD"
        Instant timestamp, // Server timestamp
        String titleKey,   // e.g. "notification.booking.approved.title"
        Object[] messageParams // e.g. ["Room A101", "Student Name"]
) {
    public NotificationPayload(String type, String title, String message, Long bookingId, String status, Instant timestamp) {
        this(null, type, title, message, bookingId, status, false, "UNREAD", timestamp, null, null);
    }

    public NotificationPayload(String type, String title, String message, Long bookingId, String status, Instant timestamp, String titleKey, Object[] messageParams) {
        this(null, type, title, message, bookingId, status, false, "UNREAD", timestamp, titleKey, messageParams);
    }

    public static NotificationPayload fromNotification(Notification notification, String title, String message) {
        return new NotificationPayload(
                notification.getId(),
                notification.getType().name(),
                title,
                message,
                notification.getRelatedId() != null ? Long.parseLong(notification.getRelatedId()) : null,
                null,
                notification.isRead(),
                notification.isRead() ? "READ" : "UNREAD",
                notification.getCreatedAt(),
                null,
                null
        );
    }
}

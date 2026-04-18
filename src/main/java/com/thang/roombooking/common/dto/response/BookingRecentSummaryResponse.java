package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.BookingStatus;

import java.time.Instant;

public record BookingRecentSummaryResponse(
        Long bookingId,
        String classroomName,
        String buildingName,
        /** Aligns with {@link com.thang.roombooking.common.enums.BookingAction} names (e.g. CHECK_OUT). */
        String action,
        BookingStatus statusAfter,
        Instant timestamp,
        String message
) {
}

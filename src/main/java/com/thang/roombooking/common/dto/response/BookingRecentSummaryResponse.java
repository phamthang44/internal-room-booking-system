package com.thang.roombooking.common.dto.response;

import java.time.Instant;

public record BookingRecentSummaryResponse(
        Long bookingId,
        String classroomName,
        String buildingName,
        String action,
        Instant timestamp,
        String message
) {
}

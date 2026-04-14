package com.thang.roombooking.common.dto.response;

import java.time.Instant;

public record BookingHistorySummaryResponse(
        Long bookingId,
        String classroomName,
        String action,         // Lấy từ BookingHistory (CHECK_IN, CANCEL,...)
        String statusAfter,
        Instant timestamp,
        String note,
        String performedBy) {
}

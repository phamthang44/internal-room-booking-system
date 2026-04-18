package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.BookingStatus;

import java.time.LocalDate;

public record BookingSummaryResponse(
        Long bookingId,
        String classroomName,
        String buildingName,
        LocalDate bookingDate,
        String timeSlotRange, // Ví dụ: "07:00 - 09:00"
        BookingStatus status,
        /** CHECK_IN when APPROVED, CHECK_OUT when CHECKED_IN; null otherwise. */
        String nextAction
) {
}

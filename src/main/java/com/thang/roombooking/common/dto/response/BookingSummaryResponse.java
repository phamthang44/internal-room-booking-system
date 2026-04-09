package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.BookingStatus;

import java.time.LocalDate;

public record BookingSummaryResponse(
        Long bookingId,
        String classroomName,
        String buildingName,
        LocalDate bookingDate,
        String timeSlotRange, // Ví dụ: "07:00 - 09:00"
        BookingStatus status
) {
}

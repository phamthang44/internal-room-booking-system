package com.thang.roombooking.common.dto.response;

import java.time.LocalTime;

public record TimeSlotResponse(
        LocalTime startTime,
        LocalTime endTime,
        String slotName) {
}

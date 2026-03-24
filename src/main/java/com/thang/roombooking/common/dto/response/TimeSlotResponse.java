package com.thang.roombooking.common.dto.response;

import java.time.LocalTime;

public record TimeSlotResponse(
        int id,
        LocalTime startTime,
        LocalTime endTime,
        String slotName) {
}

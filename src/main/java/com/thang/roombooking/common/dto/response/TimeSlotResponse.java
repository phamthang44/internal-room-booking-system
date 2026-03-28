package com.thang.roombooking.common.dto.response;

import lombok.Builder;

import java.time.LocalTime;

@Builder
public record TimeSlotResponse(
        int id,
        LocalTime startTime,
        LocalTime endTime,
        String slotName) {
}

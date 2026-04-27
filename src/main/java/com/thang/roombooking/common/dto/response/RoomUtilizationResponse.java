package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomUtilizationResponse {
    Long classroomId;
    String roomName;
    LocalDate weekStart;
    int bookedSlotCount;
    int totalSlotCapacity;
    BigDecimal utilizationPct;
    long totalBookings;
    BigDecimal avgAttendees;
}

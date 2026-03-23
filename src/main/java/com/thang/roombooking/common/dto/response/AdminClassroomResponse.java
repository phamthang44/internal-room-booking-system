package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.RoomStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record AdminClassroomResponse(
        int id,
        String buildingName,
        String roomNumber,
        int capacity,
        RoomStatus status,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy) {
}

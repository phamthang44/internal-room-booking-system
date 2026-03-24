package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.RoomStatus;

public record UpdateClassroomResponse(
        Long id,
        BasicBuildingResponse building,
        String roomName,
        int capacity,
        RoomStatus status,
        BasicRoomTypeResponse roomType
) {
}

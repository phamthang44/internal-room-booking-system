package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.enums.RoomStatus;

public record UpdateClassroomStatusRequest(
        RoomStatus status
) {
}

package com.thang.roombooking.service.policy;

import com.thang.roombooking.common.enums.RoomStatus;

public interface ChangeRoomStatusPolicy {
    void validate(Long roomId, RoomStatus currentStatus, RoomStatus newStatus);
    void validateStatusTransition(RoomStatus currentStatus, RoomStatus newStatus);
}

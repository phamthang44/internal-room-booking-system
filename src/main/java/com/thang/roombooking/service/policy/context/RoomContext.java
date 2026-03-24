package com.thang.roombooking.service.policy.context;

import com.thang.roombooking.common.enums.RoomAction;
import com.thang.roombooking.common.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class RoomContext {
    private Long roomId;
    private RoomStatus currentStatus;
    private RoomStatus newStatus;
    private RoomAction action; // Thêm action vào đây
}
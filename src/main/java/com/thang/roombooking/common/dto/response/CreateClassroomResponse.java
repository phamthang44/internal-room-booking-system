package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.RoomStatus;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateClassroomResponse {
    private Long id;
    private BasicBuildingResponse building;
    private String roomName;
    private int capacity;
    private RoomStatus status;
    private BasicRoomTypeResponse roomType;
}



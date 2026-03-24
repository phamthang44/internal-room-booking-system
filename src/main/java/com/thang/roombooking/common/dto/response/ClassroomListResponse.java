package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.RoomStatus;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ClassroomListResponse {
    private Long classroomId;
    private String buildingName;
    private String roomName;
    private int capacity;
    private RoomStatus status;
    private List<EquipmentResponse> equipments;
    private String roomType;
}

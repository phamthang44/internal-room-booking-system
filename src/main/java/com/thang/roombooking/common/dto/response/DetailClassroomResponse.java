package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DetailClassroomResponse {

    private Long classroomId;
    private String roomName;
    private BasicBuildingResponse building;
    private int capacity;
    private ClassroomAvailabilityResponse schedule;
    private List<EquipmentResponse> equipments;
    private String addressBuildingLocation;
    private BasicRoomTypeResponse roomType;
    private List<String> imageUrls;
    private List<RoomAssetResponse> roomAssets;

}

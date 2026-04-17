package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AdminDetailClassroomResponse {
    private BasicBuildingResponse building;
    private String roomName;
    private int capacity;
    private List<Instant> availableDates;
    private ClassroomAvailabilityResponse schedule;
    private List<EquipmentResponse> equipments;
    private String addressBuildingLocation;
    private BasicRoomTypeResponse roomType;
    private List<String> imageUrls;
    private List<RoomAssetResponse> roomAssets;

    //audit
    private AuditResponse auditResponse;
}

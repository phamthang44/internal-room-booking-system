package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DetailClassroomResponse {

    private String buildingName;
    private String roomNumber;
    private int capacity;
    private List<Instant> availableDates;
    private Instant month;
    private List<TimeSlotResponse> timeSlots;
    private List<EquipmentResponse> equipments;
    private String addressBuildingLocation;

}

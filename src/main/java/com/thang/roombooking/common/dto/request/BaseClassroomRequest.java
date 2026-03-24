package com.thang.roombooking.common.dto.request;

import java.util.List;

public interface BaseClassroomRequest {
    String roomName();
    Long buildingId();
    Integer capacity();
    Integer roomTypeId();
    List<EquipmentRequest> equipments();
    List<String> imageUrls();
    Boolean isActive();
}

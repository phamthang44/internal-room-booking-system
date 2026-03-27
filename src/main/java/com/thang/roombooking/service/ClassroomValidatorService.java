package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.BaseClassroomRequest;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.entity.RoomType;


public interface ClassroomValidatorService {
    record ValidationResult(Building building, RoomType roomType) {}
    ValidationResult validateAndGetEntities(BaseClassroomRequest req, Long currentId);
    void validateRoomCapacity(Long roomId, Integer numberAttendees);
    void validateRoomStatus(Long classroomId);
}

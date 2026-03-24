package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.BaseClassroomRequest;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.entity.Classroom;
import com.thang.roombooking.entity.RoomType;
import com.thang.roombooking.service.impl.ClassroomCommandServiceImpl;

import java.util.List;

public interface ClassroomValidatorService {
    record ValidationResult(Building building, RoomType roomType) {}
    ValidationResult validateAndGetEntities(BaseClassroomRequest req, Long currentId);
}

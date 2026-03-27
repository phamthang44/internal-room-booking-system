package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.BaseClassroomRequest;
import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.ClassroomErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.entity.*;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.BuildingRepository;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.repository.EquipmentRepository;
import com.thang.roombooking.repository.RoomTypeRepository;
import com.thang.roombooking.service.ClassroomValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassroomValidatorServiceImpl implements ClassroomValidatorService {

    private final ClassroomRepository classroomRepository;
    private final BuildingRepository buildingRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    public ValidationResult validateAndGetEntities(BaseClassroomRequest req, Long currentId) {
        // Rule 1: Check trùng tên nhưng loại trừ ID hiện tại (Dùng cho Update)
        boolean exists = (currentId == null)
                ? classroomRepository.existsByBuildingIdAndRoomName(req.buildingId(), req.roomName())
                : classroomRepository.existsByBuildingIdAndRoomNameAndIdNot(req.buildingId(), req.roomName(), currentId);

        if (exists) {
            throw new AppException(ClassroomErrorCode.ROOM_NAME_EXISTED);
        }

        // Rule 2 & 3: Lấy Building và RoomType
        Building building = buildingRepository.findById(req.buildingId())
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "Building ID: " +  req.buildingId()));

        if (Boolean.FALSE.equals(building.getIsActive())) {
            throw new AppException(ClassroomErrorCode.BUILDING_NOT_ACTIVE);
        }

        RoomType roomType = roomTypeRepository.findById(Long.valueOf(req.roomTypeId()))
                .orElseThrow(() -> new AppException(ClassroomErrorCode.ROOM_TYPE_NOT_FOUND));

        if (req.capacity() < roomType.getMinCapacity() || req.capacity() > roomType.getMaxCapacity()) {
            throw new AppException(ClassroomErrorCode.CAPACITY_OUT_OF_RANGE,
                    roomType.getNameKey(), roomType.getMinCapacity(), roomType.getMaxCapacity());
        }

        return new ValidationResult(building, roomType);
    }

    /**
     * Dùng cho luồng Đặt phòng (Booking): Kiểm tra sức chứa
     */
    @Override
    @Transactional(readOnly = true)
    public void validateRoomCapacity(Long roomId, Integer numberAttendees) {
        if (numberAttendees == null) return;

        Classroom classroom = classroomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ClassroomErrorCode.CLASSROOM_NOT_FOUND, roomId));

        if (numberAttendees > classroom.getCapacity()) {
            throw new AppException(ClassroomErrorCode.CAPACITY_NOT_ENOUGH, numberAttendees, classroom.getCapacity());
        }
    }


    /**
     * Dùng cho luồng Đặt phòng (Booking): Kiểm tra phòng có tồn tại, đúng tòa nhà và đang sẵn sàng không
     */
    @Override
    @Transactional(readOnly = true)
    public void validateRoomStatus(Long roomId) {
        Classroom classroom = classroomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ClassroomErrorCode.CLASSROOM_NOT_FOUND, roomId));

        Building building = classroom.getBuilding();
        if (building == null) {
            throw new AppException(CommonErrorCode.INTERNAL_ERROR, I18nUtils.get("error.classroom.no_building"));
        }

        if (Boolean.FALSE.equals(building.getIsActive())) {
            throw new AppException(ClassroomErrorCode.BUILDING_NOT_ACTIVE);
        }

        // Ưu tiên check lỗi cụ thể trước (Bảo trì)
        if (classroom.getStatus() == RoomStatus.MAINTENANCE) {
            throw new AppException(ClassroomErrorCode.CLASSROOM_UNDER_MAINTENANCE);
        }

        // Sau đó check các trạng thái không khả dụng khác
        if (classroom.getStatus() != RoomStatus.AVAILABLE) {
            throw new AppException(ClassroomErrorCode.CLASSROOM_IS_DELETED_OR_INACTIVE, classroom.getStatus().name());
        }

    }
}

package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.BaseClassroomRequest;
import com.thang.roombooking.common.dto.request.CreateClassroomRequest;
import com.thang.roombooking.common.dto.request.EquipmentRequest;
import com.thang.roombooking.common.dto.request.UpdateClassroomRequest;
import com.thang.roombooking.common.dto.response.CreateClassroomResponse;
import com.thang.roombooking.common.dto.response.UpdateClassroomResponse;
import com.thang.roombooking.common.enums.RoomAction;
import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.common.mapper.ClassroomMapper;
import com.thang.roombooking.entity.*;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.*;
import com.thang.roombooking.service.ClassroomCommandService;
import com.thang.roombooking.service.ClassroomValidatorService;
import com.thang.roombooking.service.policy.RoomPolicy;
import com.thang.roombooking.service.policy.RoomPolicyFactory;
import com.thang.roombooking.service.policy.context.RoomContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassroomCommandServiceImpl implements ClassroomCommandService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomValidatorService classroomValidatorService;
    private final ClassroomMapper classroomMapper;
    private final EquipmentRepository equipmentRepository;

    private final RoomPolicyFactory policyFactory;

    @Override
    @Transactional
    public CreateClassroomResponse createAnClassroom(CreateClassroomRequest req) {
        // Validate và lấy thực thể (ID = null vì là tạo mới)
        ClassroomValidatorService.ValidationResult res = classroomValidatorService.validateAndGetEntities(req, null);

        Classroom classroom = classroomMapper.toEntityClassroom(req);
        log.debug("New classroom with name : {}", classroom.getRoomName());
        Map<Equipment, Integer> equipmentMap = processEquipmentRequest(req);

        // Lắp ráp quan hệ
        classroom.updateDetails(res.building(), res.roomType(), equipmentMap,
                req.isActive() ? RoomStatus.AVAILABLE : RoomStatus.INACTIVE, req.imageUrls());

        Classroom saved = classroomRepository.save(classroom);
        log.info("Created Classroom: {}", saved.getRoomName());
        return classroomMapper.toCreateClassroomResponse(saved);
    }

    private Map<Equipment, Integer> processEquipmentRequest(BaseClassroomRequest req) {
        Map<Equipment, Integer> equipmentMap = new HashMap<>();
        if (req.equipments() != null && !req.equipments().isEmpty()) {
            // 1. Lấy danh sách ID từ request mới để truy vấn DB
            List<Integer> ids = req.equipments().stream()
                    .map(EquipmentRequest::id)
                    .toList();

            List<Equipment> equipments = equipmentRepository.findAllByIdIn(ids);

            if (equipments.size() != ids.size()) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, I18nUtils.get("error.equipments.not_found"));
            }

            // 2. Tạo một Map phụ để lookup số lượng nhanh từ Request: Map<ID, Quantity>
            Map<Integer, Integer> qtyMap = req.equipments().stream()
                    .collect(Collectors.toMap(EquipmentRequest::id, EquipmentRequest::quantity));

            // 3. Chuyển thành Map<Equipment, Integer> để đẩy vào Entity
            equipmentMap = equipments.stream()
                    .collect(Collectors.toMap(
                            eq -> eq,
                            eq -> qtyMap.getOrDefault(eq.getId(), 1) // Lấy đúng số lượng từ request
                    ));
        }
        return equipmentMap;
    }

    @Override
    @Transactional
    public UpdateClassroomResponse updateClassroom(UpdateClassroomRequest req) {
        // 1. Tìm bản ghi cũ trong DB (Bắt buộc)
        Classroom existingClassroom = classroomRepository.findById(req.classroomId())
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "Classroom ID: " + req.classroomId()));

        // 2. Validate (Truyền ID hiện tại để không tự chặn chính mình khi check trùng tên)
        ClassroomValidatorService.ValidationResult res = classroomValidatorService.validateAndGetEntities(req, existingClassroom.getId());

        // 3. Update dữ liệu từ Request vào Entity cũ (Dùng Mapper với @MappingTarget)
        classroomMapper.updateEntityFromRequest(req, existingClassroom);
        Map<Equipment, Integer> equipmentMap = processEquipmentRequest(req);
        // 4. Cập nhật lại các mối quan hệ (Building, RoomType, Equipments)
        existingClassroom.updateDetails(res.building(), res.roomType(), equipmentMap,
                req.isActive() ? RoomStatus.AVAILABLE : RoomStatus.INACTIVE, req.imageUrls());

        Classroom saved = classroomRepository.save(existingClassroom);
        log.info("Updated Classroom ID: {}", saved.getId());
        return classroomMapper.toUpdateClassroomResponse(saved);
    }

    @Override
    public void removeClassroom(Long id) {
        Classroom existingClassroom = classroomRepository.findById(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "Classroom ID: " + id));

        RoomPolicy policy = policyFactory.getPolicy(RoomAction.DELETE);

        policy.validate(
                new RoomContext(
                        id,
                        existingClassroom.getStatus(),
                        RoomStatus.DELETED,
                        RoomAction.DELETE)
        );

        log.info("Deleted Classroom ID: {}", existingClassroom.getId());
        classroomRepository.delete(existingClassroom);
    }

    @Override
    public UpdateClassroomResponse updateStatusClassroom(Long id, RoomStatus status) {
        Classroom existingClassroom = classroomRepository.findById(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "Classroom ID: " + id));

        RoomPolicy policy = policyFactory.getPolicy(RoomAction.CHANGE_STATUS);
        policy.validate(RoomContext.builder()
                        .currentStatus(existingClassroom.getStatus())
                        .newStatus(status)
                        .action(RoomAction.CHANGE_STATUS)
                        .roomId(existingClassroom.getId())
                .build());

        existingClassroom.setStatus(status);
        Classroom saved = classroomRepository.save(existingClassroom);
        log.info("Updated Classroom ID: {} with new Status: {}", saved.getId(), saved.getStatus());

        return classroomMapper.toUpdateClassroomResponse(saved);
    }


}

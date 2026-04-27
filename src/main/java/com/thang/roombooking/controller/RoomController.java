package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.RoomSearchRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.ClassroomListResponse;
import com.thang.roombooking.common.dto.response.DetailClassroomResponse;
import com.thang.roombooking.common.enums.BehaviorEventType;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.BehaviorTrackingService;
import com.thang.roombooking.service.ClassroomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
@Validated
public class RoomController {

    private final ClassroomQueryService classroomService;
    private final BehaviorTrackingService behaviorTrackingService;

    // ---------------------------------------------------------
    // PUBLIC API (Customer - Storefront)
    // - URL: GET /api/v1/products
    // - Forces ACTIVE status
    // - Returns ProductListingDto (lightweight)
    // ---------------------------------------------------------
    @GetMapping
    public ResponseEntity<ApiResult<List<ClassroomListResponse>>> searchPublic(
            @AuthenticationPrincipal SecurityUserDetails userDetails,
            @ModelAttribute RoomSearchRequest request) {
        log.info("Public search - keyword: {}, status: {}, capacity: {}, timeSlotIds: {}, bookingDate: {}, equipmentId: {}, sort: {}, page: {}, size: {}",
                request.getKeyword(), request.getRoomStatus(), request.getCapacity(), request.getTimeSlotIds(), request.getBookingDate(), request.getEquipmentId(), request.getSort(), request.getPage(), request.getSize());

        if (userDetails != null) {
            Long userId = userDetails.getUser().getId();
            if (request.getCapacity() > 0) {
                behaviorTrackingService.recordEvent(userId, BehaviorEventType.CAPACITY_SEARCHED, "CLASSROOM", null, String.valueOf(request.getCapacity()));
            }
            if (request.getEquipmentId() > 0) {
                behaviorTrackingService.recordEvent(userId, BehaviorEventType.EQUIPMENT_FILTER_APPLIED, "EQUIPMENT", (long) request.getEquipmentId());
            }
        }

        Page<ClassroomListResponse> roomPage = classroomService.searchPublic(request);

        return ResponseEntity.ok(ApiResult.success(
                roomPage.getContent(),
                request.getPage(),
                request.getSize(),
                roomPage.getTotalElements()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<DetailClassroomResponse>> retrieveRoomDetailById(
            @AuthenticationPrincipal SecurityUserDetails userDetails,
            @PathVariable Long id) {
        log.info("Received retrieve room detail - id: {}", id);

        if (userDetails != null) {
            behaviorTrackingService.recordEvent(userDetails.getUser().getId(), BehaviorEventType.ROOM_VIEWED, "CLASSROOM", id);
        }

        DetailClassroomResponse response = classroomService.getDetailClassroom(id);

        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("rooms.detail.retrieve.success")));
    }

}

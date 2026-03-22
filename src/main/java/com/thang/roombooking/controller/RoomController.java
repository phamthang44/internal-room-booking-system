package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.RoomSearchRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.BasicClassroomResponse;
import com.thang.roombooking.service.ClassroomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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

    // ---------------------------------------------------------
    // PUBLIC API (Customer - Storefront)
    // - URL: GET /api/v1/products
    // - Forces ACTIVE status
    // - Returns ProductListingDto (lightweight)
    // ---------------------------------------------------------
    @GetMapping
    public ResponseEntity<ApiResult<List<BasicClassroomResponse>>> searchPublic(
            @ModelAttribute RoomSearchRequest request) {
        log.info("Public search - keyword: {}, status: {}, capacity : {}, time slot id : {}, booking date : {}, filter by equipment : {}, sort: {}, page: {}, size: {}",
                request.getKeyword(), request.getRoomStatus(), request.getCapacity(), request.getTimeSlotId(), request.getBookingDate(), request.getEquipmentId(), request.getSort(), request.getPage(), request.getSize());

        Page<BasicClassroomResponse> roomPage = classroomService.searchPublic(request);

        return ResponseEntity.ok(ApiResult.success(
                roomPage.getContent(),
                request.getPage(),
                request.getSize(),
                roomPage.getTotalElements()
        ));
    }


}

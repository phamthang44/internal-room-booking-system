package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.CreateClassroomRequest;
import com.thang.roombooking.common.dto.request.UpdateClassroomRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.CreateClassroomResponse;
import com.thang.roombooking.common.dto.response.UpdateClassroomResponse;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.service.ClassroomCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/rooms")
@Validated
public class AdminRoomController {

    private final ClassroomCommandService classroomCommandService;


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<CreateClassroomResponse>> createAnClassroom(@Valid @RequestBody CreateClassroomRequest req) {
        log.info("Received request to create classroom {}", req.roomName());
        var response = classroomCommandService.createAnClassroom(req);
        return  ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResult.success(response, I18nUtils.get("classroom.created.success", response.getRoomName()))
        );
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<UpdateClassroomResponse>> updateClassroom(@Valid @RequestBody UpdateClassroomRequest req) {
        log.info("Received request to update classroom {}", req.classroomId());
        var response = classroomCommandService.updateClassroom(req);
        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("classroom.updated.success", response.roomName())));
    }

}

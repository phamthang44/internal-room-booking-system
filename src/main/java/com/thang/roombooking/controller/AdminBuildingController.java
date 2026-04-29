package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.CreateBuildingRequest;
import com.thang.roombooking.common.dto.request.UpdateBuildingRequest;
import com.thang.roombooking.common.dto.response.AdminBuildingResponse;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.service.BuildingCommandService;
import com.thang.roombooking.service.BuildingQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/building")
@Validated
public class AdminBuildingController {

    private final BuildingCommandService buildingCommandService;
    private final BuildingQueryService buildingQueryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<List<AdminBuildingResponse>>> getAllBuildings() {
        return ResponseEntity.ok(ApiResult.success(buildingQueryService.retrieveAllBuildings()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<AdminBuildingResponse>> getBuildingById(
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResult.success(buildingQueryService.getBuildingById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResult<AdminBuildingResponse>> createBuilding(
            @Valid @RequestBody CreateBuildingRequest req) {
        AdminBuildingResponse response = buildingCommandService.createBuilding(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(response, I18nUtils.get("building.created.success")));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResult<AdminBuildingResponse>> updateBuilding(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateBuildingRequest req) {
        AdminBuildingResponse response = buildingCommandService.updateBuilding(id, req);
        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("building.updated.success")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResult<Void>> deleteBuilding(
            @PathVariable @Positive Long id) {
        buildingCommandService.deleteBuilding(id);
        return ResponseEntity.ok(ApiResult.success(null, I18nUtils.get("building.deleted.success")));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResult<AdminBuildingResponse>> toggleStatus(
            @PathVariable @Positive Long id,
            @RequestParam boolean isActive) {
        AdminBuildingResponse response = buildingCommandService.toggleStatus(id, isActive);
        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("building.status.updated.success")));
    }
}

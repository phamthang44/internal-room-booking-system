package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.CreateEquipmentRequest;
import com.thang.roombooking.common.dto.request.UpdateEquipmentRequest;
import com.thang.roombooking.common.dto.response.AdminEquipmentDetailResponse;
import com.thang.roombooking.common.dto.response.AdminEquipmentListResponse;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.service.EquipmentCommandService;
import com.thang.roombooking.service.EquipmentQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/equipment")
@Validated
public class AdminEquipmentController {

    private final EquipmentQueryService equipmentQueryService;
    private final EquipmentCommandService equipmentCommandService;

    // ── GET /api/v1/admin/equipment?keyword=&page=0&size=20&sort=nameKey,asc ─
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResult<List<AdminEquipmentListResponse>>> listEquipment(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        log.info("Admin list equipment | keyword={} | page={} | size={}", keyword, pageable.getPageNumber(), pageable.getPageSize());
        Page<AdminEquipmentListResponse> result = equipmentQueryService.listEquipment(keyword, pageable);
        return ResponseEntity.ok(ApiResult.successPage(result, I18nUtils.get("equipment.list.success")));
    }

    // ── GET /api/v1/admin/equipment/{id} ─────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResult<AdminEquipmentDetailResponse>> getEquipment(
            @PathVariable @Positive(message = "{validation.id.must_be_positive}") Integer id) {

        log.info("Admin get equipment | id={}", id);
        AdminEquipmentDetailResponse response = equipmentQueryService.getEquipment(id);
        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("equipment.detail.success")));
    }

    // ── POST /api/v1/admin/equipment ─────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<AdminEquipmentDetailResponse>> createEquipment(
            @Valid @RequestBody CreateEquipmentRequest req) {

        log.info("Admin create equipment | nameEn={}", req.nameEn());
        AdminEquipmentDetailResponse response = equipmentCommandService.createEquipment(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(response, I18nUtils.get("equipment.created.success")));
    }

    // ── PUT /api/v1/admin/equipment/{id} ─────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<AdminEquipmentDetailResponse>> updateEquipment(
            @PathVariable @Positive(message = "{validation.id.must_be_positive}") Integer id,
            @Valid @RequestBody UpdateEquipmentRequest req) {

        log.info("Admin update equipment | id={}", id);
        AdminEquipmentDetailResponse response = equipmentCommandService.updateEquipment(id, req);
        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("equipment.updated.success")));
    }

    // ── DELETE /api/v1/admin/equipment/{id} ──────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<String>> deactivateEquipment(
            @PathVariable @Positive(message = "{validation.id.must_be_positive}") Integer id) {

        log.info("Admin deactivate equipment | id={}", id);
        equipmentCommandService.deactivateEquipment(id);
        return ResponseEntity.ok(ApiResult.success(I18nUtils.get("equipment.deactivated.success")));
    }

    // ── PATCH /api/v1/admin/equipment/{id}/reactivate ────────────────────────
    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<String>> reactivateEquipment(
            @PathVariable @Positive(message = "{validation.id.must_be_positive}") Integer id) {

        log.info("Admin reactivate equipment | id={}", id);
        equipmentCommandService.reactivateEquipment(id);
        return ResponseEntity.ok(ApiResult.success(I18nUtils.get("equipment.reactivated.success")));
    }
}

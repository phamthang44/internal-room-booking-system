package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.PenaltyExtendRequest;
import com.thang.roombooking.common.dto.request.PenaltyRevokeRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.PenaltyRecordResponse;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.PenaltyCommandService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/admin/penalties")
@RequiredArgsConstructor
public class AdminPenaltyController {

    private final PenaltyCommandService penaltyCommandService;

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<PenaltyRecordResponse>> revokePenalty(
            @Positive(message = "{validation.id.must_be_positive}") @PathVariable Long id,
            @Valid @RequestBody PenaltyRevokeRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        
        var response = penaltyCommandService.revokePenalty(id, req, userDetails.getUser());
        
        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("penalty.revoke.success")));
    }

    @PostMapping("/{id}/extend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<PenaltyRecordResponse>> extendPenalty(
            @Positive(message = "{validation.id.must_be_positive}") @PathVariable Long id,
            @Valid @RequestBody PenaltyExtendRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {

        var response = penaltyCommandService.extendPenalty(id, req, userDetails.getUser());

        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("penalty.extend.success")));
    }



}

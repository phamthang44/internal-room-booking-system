package com.thang.roombooking.controller;
 
import com.thang.roombooking.common.dto.request.CreateViolationRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.ViolationResponse;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.service.ViolationCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api/v1/admin/violations")
@RequiredArgsConstructor
public class AdminViolationController {
 
    private final ViolationCommandService violationCommandService;
 
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<ViolationResponse>> createViolation(
            @Valid @RequestBody CreateViolationRequest request,
            @AuthenticationPrincipal UserAccount admin) {
        
        ViolationResponse response = violationCommandService.createManualViolation(request, admin);
        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("violation.create.success")));
    }
}

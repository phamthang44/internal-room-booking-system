package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.UpdateProfileRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.PenaltyRecordResponse;
import com.thang.roombooking.common.dto.response.UserProfileResponse;
import com.thang.roombooking.common.dto.response.ViolationResponse;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.PenaltyQueryService;
import com.thang.roombooking.service.UserCommandService;
import com.thang.roombooking.service.UserQueryService;
import com.thang.roombooking.service.ViolationQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/profile")
@Validated
public class UserController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final PenaltyQueryService penaltyQueryService;
    private final ViolationQueryService violationQueryService;
    public static final String USER_RETRIEVE_MESSAGE = "user.profile.retrieve.success";
    public static final String USER_UPDATE_MESSAGE = "user.profile.update.success";

    @GetMapping
    public ResponseEntity<ApiResult<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal SecurityUserDetails currentUser) {

        var response = userQueryService.getProfile(currentUser);

        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get(USER_RETRIEVE_MESSAGE)));
    }

    @GetMapping("/penalties")
    public ResponseEntity<ApiResult<List<PenaltyRecordResponse>>> getCurrentUserPenalties(
            @AuthenticationPrincipal SecurityUserDetails currentUser,
            Pageable pageable) {

        Page<PenaltyRecordResponse> response = penaltyQueryService.getUserPenaltyHistory(currentUser.getUser().getId(), pageable);

        return ResponseEntity.ok(ApiResult.successPage(response, I18nUtils.get(USER_RETRIEVE_MESSAGE)));
    }

    @GetMapping("/violations")
    public ResponseEntity<ApiResult<List<ViolationResponse>>> getCurrentUserViolations(
            @AuthenticationPrincipal SecurityUserDetails currentUser,
            Pageable pageable) {

        Page<ViolationResponse> response = violationQueryService.getUserViolationHistory(currentUser.getUser().getId(), pageable);

        return ResponseEntity.ok(ApiResult.successPage(response, I18nUtils.get(USER_RETRIEVE_MESSAGE)));
    }

    @PutMapping
    public ResponseEntity<ApiResult<UserProfileResponse>> updateUserProfile(
            @AuthenticationPrincipal SecurityUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {

        var response = userCommandService.updateProfile(currentUser.getUser().getId(), request);

        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get(USER_UPDATE_MESSAGE)));
    }

}

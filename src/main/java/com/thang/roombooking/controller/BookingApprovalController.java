package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.ApprovalSearchRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.ApprovalSummaryResponse;
import com.thang.roombooking.service.BookingApprovalQueryService;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings/approvals")
@Validated
public class BookingApprovalController {

    private final BookingApprovalQueryService bookingApprovalQueryService;

    @GetMapping
    public ResponseEntity<ApiResult<List<ApprovalSummaryResponse>>> getApprovals(
            @Valid ApprovalSearchRequest request,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {

        log.info("Received request to get approval history | userId: {} | keyword: {}",
                userDetails.getUser().getId(), request.getKeyword());

        return ResponseEntity.ok(bookingApprovalQueryService.searchApprovals(request, userDetails.getUser()));
    }

}

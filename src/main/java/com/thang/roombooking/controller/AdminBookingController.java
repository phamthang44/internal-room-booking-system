package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.BookingApprovalRequest;
import com.thang.roombooking.common.dto.request.CheckInRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.enums.ApprovalAction;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.BookingCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/bookings")
@Validated
public class AdminBookingController {

    private final BookingCommandService bookingCommandService;

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<String>> approveBooking(
            @Valid @RequestBody BookingApprovalRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Received request to approve booking id {}, staff/admin id {}",
                req.bookingId(), userDetails.getUser().getEmail());

        bookingCommandService.approveBooking(req, userDetails.getUser());

        return  ResponseEntity.status(HttpStatus.OK).body(
                ApiResult.success(I18nUtils.get("booking.approved.success"))
        );
    }

}

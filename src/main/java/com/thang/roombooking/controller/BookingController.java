package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.CreateBookingRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.idempotency.config.Idempotent;
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
@RequestMapping("/api/v1/bookings")
@Validated
public class BookingController {

    private final BookingCommandService bookingCommandService;

    @Idempotent(keyPrefix = "booking-create")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResult<CreateBookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Received request to create booking for classroom id {}, student id {}, timeslot id {}, booking date {}",
                req.classroomId(), userDetails.getUser().getEmail(), req.timeSlotIds(), req.bookingDate());

        var response = bookingCommandService.createBooking(req, userDetails.getUser());

        return  ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResult.success(response, I18nUtils.get("booking.created.success", response.getBookingId()))
        );
    }


}

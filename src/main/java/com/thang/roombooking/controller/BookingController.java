package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.CheckInRequest;
import com.thang.roombooking.common.dto.request.CreateBookingRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.idempotency.config.Idempotent;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.BookingCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
@Validated
public class BookingController {

    private final BookingCommandService bookingCommandService;

    @Idempotent(keyPrefix = "booking-create")
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'STAFF')")
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

    @Idempotent(keyPrefix = "booking-checkin")
    @PostMapping("/checkin")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<String>> checkIn(
            @Valid @RequestBody CheckInRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Received request to checkin booking id {}, student id {}",
                req.bookingId(), userDetails.getUser().getEmail());

        bookingCommandService.checkIn(req, userDetails.getUser());

        return  ResponseEntity.status(HttpStatus.OK).body(
                ApiResult.success(I18nUtils.get("booking.checkin.success"))
        );
    }

//    @PostMapping("/cancel")
//    @PreAuthorize("hasRole('STUDENT')")
//    public ResponseEntity<ApiResult<CreateBookingResponse>> cancelBooking(
//            @Valid @RequestBody CancelBookingRequest req,
//            @AuthenticationPrincipal SecurityUserDetails userDetails) {
//        log.info("Received request to cancel booking id {}, student id {}",
//                req.bookingId(), userDetails.getUser().getEmail());
//
//        var response = bookingCommandService.cancelBooking(req, userDetails.getUser());
//
//        return  ResponseEntity.status(HttpStatus.OK).body(
//                ApiResult.success(response, I18nUtils.get("booking.cancel.success", response.getBookingId()))
//        );
//    }


//    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('STUDENT')")
//    public ResponseEntity<ApiResult<CreateBookingResponse>> getBookingDetailInformation(
//            @Min(value = 1, message = "{validation.booking.id.min}") @PathVariable Long id,
//            @AuthenticationPrincipal SecurityUserDetails userDetails) {
//        log.info("Received request to get detail booking id {}, student id {}",
//                id, userDetails.getUser().getEmail());
//
//        var response = bookingQueryService.getBooking(id, userDetails.getUser());
//
//        return  ResponseEntity.status(HttpStatus.OK).body(
//                ApiResult.success(response, I18nUtils.get("booking.retrieved.success", response.getBookingId()))
//        );
//    }

//    @GetMapping
//    @PreAuthorize("hasRole('STUDENT')")
//    public ResponseEntity<ApiResult<CreateBookingResponse>> searchBookingPublic(
//            @ModelAttribute BookingSearchRequest request,
//            @AuthenticationPrincipal SecurityUserDetails userDetails) {
//        log.info("Public search - keyword: {}, status: {}, capacity : {}, time slot id : {}, booking date : {}, filter by equipment : {}, sort: {}, page: {}, size: {}",
//
//        var response = bookingQueryService.searchPublic(id, userDetails.getUser());
//
//        return  ResponseEntity.status(HttpStatus.OK).body(
//                ApiResult.success(response, I18nUtils.get("booking.retrieved.success", response.getBookingId()))
//        );
//    }




}

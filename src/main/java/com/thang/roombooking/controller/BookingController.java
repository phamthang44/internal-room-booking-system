package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.BookingSearchRequest;
import com.thang.roombooking.common.dto.request.CancelBookingRequest;
import com.thang.roombooking.common.dto.request.CheckInRequest;
import com.thang.roombooking.common.dto.request.CreateBookingRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.BookingDetailResponse;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.idempotency.config.Idempotent;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.BookingCommandService;
import com.thang.roombooking.service.BookingQueryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
@Validated
public class BookingController {

    private final BookingCommandService bookingCommandService;
    private final BookingQueryService bookingQueryService;

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Idempotent(keyPrefix = "booking-create")
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<CreateBookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Received request to create booking for classroom id {}, student id {}, timeslot id {}, booking date {}",
                req.classroomId(), userDetails.getUser().getEmail(), req.timeSlotIds(), req.bookingDate());

        var response = bookingCommandService.createBooking(req, userDetails.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResult.success(response, I18nUtils.get("booking.created.success", response.getBookingId()))
        );
    }

    // ── CHECK-IN ─────────────────────────────────────────────────────────────

    @Idempotent(keyPrefix = "booking-checkin")
    @PatchMapping("/checkin")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<String>> checkIn(
            @Valid @RequestBody CheckInRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Received request to checkin booking id {}, student id {}",
                req.bookingId(), userDetails.getUser().getEmail());

        bookingCommandService.checkIn(req, userDetails.getUser());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResult.success(I18nUtils.get("booking.checkin.success"))
        );
    }

    //
    @Idempotent(keyPrefix = "booking-cancel")
    @PatchMapping("/cancel")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<String>> cancelBooking(
            @Valid @RequestBody CancelBookingRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Received request to cancel booking id {}, student id {}, cancel time: {}",
                req.bookingId(), userDetails.getUser().getEmail(), req.cancelTime());

        bookingCommandService.cancelBooking(req.bookingId(), userDetails.getUser());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResult.success(I18nUtils.get("booking.cancel.success"))
        );
    }

    // ── GET DETAIL ───────────────────────────────────────────────────────────

    /**
     * Returns the full details of a single booking.
     * <p>
     * Accessible by the booking owner (STUDENT) as well as ADMIN and STAFF roles.
     *
     * @param id          booking primary key
     * @param userDetails authenticated principal
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<BookingDetailResponse>> getBookingDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Received request to get booking detail | bookingId={} | userId={}",
                id, userDetails.getUser().getEmail());

        BookingDetailResponse response = bookingQueryService.getBookingDetail(id, userDetails.getUser());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResult.success(response, I18nUtils.get("booking.retrieved.success", response.getBookingId()))
        );
    }

    // ── SEARCH PUBLIC ─────────────────────────────────────────────────────────

    /**
     * Paginated search over the current user's own bookings.
     * <p>
     * Query parameters:
     * <ul>
     *   <li>{@code keyword}     – optional free-text filter on room / building name</li>
     *   <li>{@code bookingDate} – optional date filter (yyyy-MM-dd)</li>
     *   <li>{@code status}      – optional {@link com.thang.roombooking.common.enums.BookingStatus} filter</li>
     *   <li>{@code timeSlotId}  – optional time-slot ID filter</li>
     *   <li>{@code attendees}   – optional minimum attendees filter</li>
     *   <li>{@code page}        – 1-indexed page number (default 1)</li>
     *   <li>{@code size}        – page size (default 20)</li>
     *   <li>{@code sort}        – one of: newest, booking_date_asc, booking_date_desc,
     *                             status_asc, status_desc (default newest)</li>
     * </ul>
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<List<BookingDetailResponse>>> searchPublic(
            @ModelAttribute BookingSearchRequest request,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Public search | status={} | date={} | timeSlotId={} | sort={} | page={} | size={}",
                request.getStatus(), request.getBookingDate(), request.getTimeSlotId(),
                request.getSort(), request.getPage(), request.getSize());

        // Service already wraps the result in ApiResult with pagination meta
        return ResponseEntity.ok(bookingQueryService.searchPublic(request, userDetails.getUser()));
    }
}

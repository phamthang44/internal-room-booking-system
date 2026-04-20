package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.AdminBookingSearchRequest;
import com.thang.roombooking.common.dto.request.BookingApprovalRequest;
import com.thang.roombooking.common.dto.response.AdminBookingDetailResponse;
import com.thang.roombooking.common.dto.response.AdminBookingListResponse;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.BookingCommandService;
import com.thang.roombooking.service.BookingQueryService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/admin/bookings")
@Validated
public class AdminBookingController {

    private final BookingCommandService bookingCommandService;
    private final BookingQueryService bookingQueryService;

    @PatchMapping("/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<String>> approveBooking(
            @Valid @RequestBody BookingApprovalRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Received request to approve booking id {}, staff/admin id {}",
                req.bookingId(), userDetails.getUser().getEmail());

        var bookingId = bookingCommandService.approveBooking(req, userDetails.getUser());

        return  ResponseEntity.status(HttpStatus.OK).body(
                ApiResult.success(I18nUtils.get("booking.approved.success", bookingId))
        );
    }

    @PatchMapping("/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<String>> rejectBooking(
            @Valid @RequestBody BookingApprovalRequest req,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Received request to reject booking id {}, staff/admin id {}",
                req.bookingId(), userDetails.getUser().getEmail());

        bookingCommandService.rejectBooking(req, userDetails.getUser());

        return ResponseEntity.ok(ApiResult.success(I18nUtils.get("booking.rejected.success", req.bookingId())));
    }

    /**
     * Paginated admin search over all bookings.
     * <p>
     * Query parameters: primary {@code bookingId} or {@code studentCode}; {@code classroomId} from a dropdown;
     * optional {@code bookingDate}, {@code status}, {@code timeSlotId},
     * {@code attendees} (minimum; {@code 0} = no filter), {@code page}, {@code size}, {@code sort}
     * (same values as public booking search).
     * <p>
     * Each list row includes only the first time slot (by start time) when multiple slots are booked;
     * use {@link #getAdminBookingDetail} for the full slot list.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<List<AdminBookingListResponse>>> searchAdmin(
            @ModelAttribute AdminBookingSearchRequest request,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Admin booking search | bookingId={} | studentCode={} | classroomId={} | status={} | date={} | page={} | size={}",
                request.getBookingId(), request.getStudentCode(), request.getClassroomId(),
                request.getStatus(), request.getBookingDate(),
                request.getPage(), request.getSize());

        return ResponseEntity.ok(bookingQueryService.searchAdmin(request, userDetails.getUser()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<AdminBookingDetailResponse>> getAdminBookingDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        log.info("Admin booking detail | bookingId={} | adminUserId={}", id, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResult.success(bookingQueryService.getAdminBookingDetail(id, userDetails.getUser())));
    }

}

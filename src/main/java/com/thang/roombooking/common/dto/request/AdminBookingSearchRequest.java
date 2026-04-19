package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.enums.BookingSort;
import com.thang.roombooking.common.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Filter / pagination request for the admin booking search endpoint.
 * <p>
 * Primary filters (indexed-friendly): {@link #bookingId} for a direct lookup, or
 * {@link #studentCode} for a narrow user match. Prefer {@link #classroomId} from a dropdown
 * instead of free-text room search.
 * <p>
 * All fields are optional – missing values mean "no filter applied".
 */
@Getter
@Setter
public class AdminBookingSearchRequest {

    /** Booking primary key; use for fastest exact lookup. */
    private Long bookingId;

    /**
     * Match on {@link com.thang.roombooking.entity.UserAccount#getStudentCode()} (case-insensitive contains).
     * Prefer this over scanning by room name.
     */
    private String studentCode;

    /** Filter by classroom; supply from a dropdown (see classroom list APIs). */
    private Long classroomId;

    /** Filter by a specific booking date. */
    private LocalDate bookingDate;

    /** Filter by booking status (PENDING, APPROVED, …). */
    private BookingStatus status;

    /** Filter by time-slot ID. */
    private Integer timeSlotId;

    /** Minimum requested attendees; {@code 0} means no filter (same as public search). */
    private int attendees;

    // ── Pagination ───────────────────────────────────────────────────────────
    /** 1-indexed page number; defaults to 1. */
    private int page = 1;

    private int size = 20;

    /** Sort strategy; defaults to NEWEST. */
    private BookingSort sort = BookingSort.NEWEST;
}

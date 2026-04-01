package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.enums.BookingSort;
import com.thang.roombooking.common.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Filter / pagination request for the public booking search endpoint.
 * All fields are optional – missing values mean "no filter applied".
 */
@Getter
@Setter
public class BookingSearchRequest {

    /** Free-text search on room name or building name. */
    private String keyword;

    /** Filter by a specific booking date. */
    private LocalDate bookingDate;

    /** Filter by booking status (PENDING, APPROVED, …). */
    private BookingStatus status;

    /** Filter by time-slot ID. */
    private Integer timeSlotId;

    /** Minimum number of attendees (capacity filter). */
    private int attendees;

    // ── Pagination ───────────────────────────────────────────────────────────
    /** 1-indexed page number; defaults to 1. */
    private int page = 1;

    private int size = 20;

    /** Sort strategy; defaults to NEWEST. */
    private BookingSort sort = BookingSort.NEWEST;
}

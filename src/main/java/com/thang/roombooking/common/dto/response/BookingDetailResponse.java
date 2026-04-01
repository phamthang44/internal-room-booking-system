package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Full detail DTO for a single Booking.
 * Returned by GET /api/v1/bookings/{id} and booking search results.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {

    @Schema(description = "Booking primary ID", example = "42")
    private Long bookingId;

    // ── Location ────────────────────────────────────────────────────────────
    @Schema(description = "Room name inside the building", example = "Lab 3A")
    private String roomName;

    @Schema(description = "Translated building name based on request locale", example = "Building A")
    private String buildingName;

    @Schema(description = "Physical address of the building", example = "123 Nguyen Van Cu, Q5")
    private String buildingAddress;

    // ── Date & Time ──────────────────────────────────────────────────────────
    @Schema(description = "Booking date (yyyy-MM-dd)", example = "2026-04-01")
    private LocalDate bookingDate;

    @Schema(description = "Ordered list of time slots for this booking")
    private List<TimeSlotResponse> timeSlots;

    // ── Booking info ─────────────────────────────────────────────────────────
    @Schema(description = "Purpose / reason for the booking")
    private String purpose;

    @Schema(description = "Number of attendees", example = "30")
    private Integer attendees;

    @Schema(description = "Current booking status")
    private BookingStatus status;

    // ── Audit / approval history ─────────────────────────────────────────────
    @Schema(description = "Approval audit history (may be empty for PENDING bookings)")
    private List<BookingApprovalResponse> approvalHistory;
}

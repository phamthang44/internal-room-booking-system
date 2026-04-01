package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.time.Instant;

/**
 * Represents a single approval/audit record for a booking.
 * Used inside {@link BookingDetailResponse} for audit history.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingApprovalResponse {

    private Long approvalId;

    /** Full name or email of the approver (staff/admin). */
    private String approverName;

    /** APPROVED or REJECTED */
    private String approvalStatus;

    /** Optional note left by the approver. */
    private String note;

    private Instant decidedAt;
}

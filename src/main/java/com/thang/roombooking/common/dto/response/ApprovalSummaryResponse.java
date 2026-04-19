package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalSummaryResponse {

    private Long approvalId;

    private Long bookingId;

    /** Full name of the student who booked the room. */
    private String studentName;

    /** Room number or name. */
    private String roomName;

    /** The date the booking is scheduled for. */
    private LocalDate bookingDate;

    /** APPROVED or REJECTED */
    private String approvalStatus;

    /** Full name or email of the approver. */
    private String approverName;

    /** Optional note or rejection reason. */
    private String note;

    /** Timestamp when the decision was made. */
    private Instant decidedAt;
}

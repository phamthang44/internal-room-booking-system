package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.PenaltyAction;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PenaltyRecordResponse {
    private Long id;
    private AuditResponse auditResponse;
    private UserBasicResponse userBasicResponse;
    private PenaltyAction action;
    private boolean isActive;
    private Instant startDate;
    private Instant endDate;
    private String reason;
}

package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentViolationSummary {
    Long violationId;
    String userEmail;
    String studentCode;
    String violationType;
    Integer severityPoints;
    Instant createdAt;
}

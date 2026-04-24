package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.ViolationSource;
import com.thang.roombooking.common.enums.ViolationType;
import lombok.Builder;
import java.time.Instant;

@Builder
public record ViolationResponse(
    Long id,
    Long userId,
    Long bookingId,
    ViolationType type,
    ViolationSource source,
    Integer severityPoints,
    String reason,
    Instant createdAt
) {}

package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBookingTrendResponse {
    LocalDate date;
    long totalBookings;
    long pendingCount;
    long approvedCount;
    long rejectedCount;
    long cancelledCount;
    long completedCount;
    long checkedInCount;
    long newViolations;
    long noShowCount;
}

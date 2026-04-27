package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardOverviewResponse {
    long pendingApprovalCount;
    long bookingsTodayCount;
    long activeRoomsCount;
    long activePenaltyCount;
    Map<String, Long> bookingStatusBreakdown;
    List<RecentViolationSummary> recentViolations;
}

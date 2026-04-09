package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class StudentDashboardResponse {
    Long totalBookings;
    Long upcomingBookings;
    Long pendingBookings;

    List<BookingSummaryResponse> upcomingList;
    List<BookingRecentSummaryResponse> historyList;

}

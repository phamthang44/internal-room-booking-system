package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.PenaltyAction;
import lombok.*;

import java.time.Instant;
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

    // Attendance stats
    Double attendanceRate;
    Long noShowCount;
    Long cancelledThisMonthCount;

    // Penalty awareness
    Boolean hasPenalty;
    PenaltyAction penaltyLevel;
    Instant penaltyExpiresAt;

    // Booking quality
    Double avgActualAttendees;
}

package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.AttendanceStatus;
import com.thang.roombooking.common.enums.BookingStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminBookingDetailResponse {

    private Long id;
    private UserBasicResponse user;
    private AdminBookingRoomRequestedResponse adminClassroom;
    private String purpose;
    private String rejectedReason;
    private LocalDate bookingDate;
    private BookingStatus status;
    private List<TimeSlotResponse> timeSlots;
    private AuditResponse audit;
    private String cancelledBy;
    private AttendanceStatus attendanceStatus;
    private Instant checkInTime;
    private Instant checkOutTime;

}

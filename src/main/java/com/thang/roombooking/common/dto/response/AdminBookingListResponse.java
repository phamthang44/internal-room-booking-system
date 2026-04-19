package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.BookingStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminBookingListResponse {

    private Long id;
    private String studentName;
    private AdminBookingRoomRequestedResponse room;
    private String purpose;
    private LocalDate date;
    private TimeSlotResponse timeSlot;
    private BookingStatus status;

}

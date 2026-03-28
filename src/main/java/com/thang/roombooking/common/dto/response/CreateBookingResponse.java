package com.thang.roombooking.common.dto.response;


import com.thang.roombooking.common.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class CreateBookingResponse {

    @Schema(description = "ID", example = "1")
    private Long bookingId;

    private String roomName;
    private BasicBuildingResponse building;

    @Schema(description = "Date", example = "year-month-date")
    private LocalDate bookingDate;

    private List<TimeSlotResponse> timeSlots;
    private BookingStatus bookingStatus;

}

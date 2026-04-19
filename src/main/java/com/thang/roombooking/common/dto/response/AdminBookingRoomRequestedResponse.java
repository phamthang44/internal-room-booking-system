package com.thang.roombooking.common.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminBookingRoomRequestedResponse {
    private String roomName;
    private int capacity;
    private int actualAttendees;
    private int requestedAttendees;
}

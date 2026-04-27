package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.RoomStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRecommendationResponse {
    Long classroomId;
    String roomName;
    String buildingName;
    String roomTypeName;
    Integer capacity;
    RoomStatus status;
    int bookingCount;
    BigDecimal avgActualAttendees;
    String reasonKey;
}

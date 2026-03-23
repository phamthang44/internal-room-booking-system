package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.enums.RoomSort;
import com.thang.roombooking.common.enums.RoomStatus;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class RoomSearchRequest {

    private String keyword;
    private RoomStatus roomStatus;
    @FutureOrPresent(message = "{validation.date.future_or_present}")
    private LocalDate bookingDate;
    private Integer timeSlotId;
    private int capacity;
    private int equipmentId;

    private int page = 0;
    private int size = 20;

    private RoomSort sort = RoomSort.NEWEST;

}

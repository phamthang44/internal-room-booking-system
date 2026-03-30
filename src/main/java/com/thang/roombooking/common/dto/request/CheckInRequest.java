package com.thang.roombooking.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CheckInRequest(

        @Schema(description = "Booking ID", example = "1")
        @NotNull(message = "{validation.booking.id.required}")
        Long bookingId,

        @NotNull(message = "{validation.booking.check_in.time_booking.required}")
        @Schema(description = "Time at the check in booking moment")
        LocalTime checkInTime
) {
}

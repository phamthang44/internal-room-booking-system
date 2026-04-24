package com.thang.roombooking.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CheckInRequest(

        @NotNull(message = "{validation.booking.check_in.time_booking.required}")
        @Schema(description = "Time at the check in booking moment")
        Instant checkInTime
) {
}

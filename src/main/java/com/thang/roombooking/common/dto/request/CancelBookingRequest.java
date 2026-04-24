package com.thang.roombooking.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CancelBookingRequest(

        @NotNull(message = "{cancel.booking.reason.required}")
        String cancelReason,

        @NotNull(message = "{validation.booking.cancel.time_booking.required}")
        @Schema(description = "Time at the cancel moment")
        Instant cancelTime
) {
}

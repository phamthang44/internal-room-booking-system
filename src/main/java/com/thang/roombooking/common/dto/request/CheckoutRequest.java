package com.thang.roombooking.common.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CheckoutRequest(
        @NotNull(message = "{validation.booking.check_out.time_booking.required}") Instant checkoutTime,
        @Min(value = 1, message = "{validation.attendees.min}")
        @Max(value = 500, message = "{validation.attendees.max}")
        Integer actualAttendees
) {}


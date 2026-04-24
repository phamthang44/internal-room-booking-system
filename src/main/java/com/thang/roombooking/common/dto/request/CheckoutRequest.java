package com.thang.roombooking.common.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CheckoutRequest(
        @NotNull(message = "{validation.booking.check_out.time_booking.required}") Instant checkoutTime
) {}


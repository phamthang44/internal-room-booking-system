package com.thang.roombooking.common.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CheckoutRequest(
        @NotNull Long bookingId,
        @NotNull Instant checkoutTime
) {}


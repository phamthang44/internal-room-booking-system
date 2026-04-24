package com.thang.roombooking.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PenaltyExtendRequest(
        @NotBlank(message = "{penalty.extend.reason.required}") String reason,

        @NotNull(message = "{validation.penalty.end_date.required}")
        @Schema(description = "The new end date for the penalty")
        Instant newEndDate
) {
}

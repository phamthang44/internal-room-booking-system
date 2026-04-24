package com.thang.roombooking.common.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PenaltyRevokeRequest(
        @NotBlank(message = "{penalty.revoke.reason.required}") String reason
) {
}

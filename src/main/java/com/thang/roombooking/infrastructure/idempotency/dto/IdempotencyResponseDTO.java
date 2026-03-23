package com.thang.roombooking.infrastructure.idempotency.dto;

import lombok.Builder;

@Builder
public record IdempotencyResponseDTO(
    int statusCode,
    String body,
    String contentType
) {
}

package com.thang.roombooking.common.dto.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ResetPasswordResult(
        String email,
        String username,
        Instant timeStamp
) {
}


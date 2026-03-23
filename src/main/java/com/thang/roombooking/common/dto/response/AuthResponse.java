package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.AuthStatus;
import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        AuthStatus status
) {
}

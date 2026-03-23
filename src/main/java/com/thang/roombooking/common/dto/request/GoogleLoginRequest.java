package com.thang.roombooking.common.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
    @NotBlank(message = "ID token cannot be blank") 
    String idToken
) {
}

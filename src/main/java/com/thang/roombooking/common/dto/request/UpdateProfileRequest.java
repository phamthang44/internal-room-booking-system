package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "{validation.auth.fullName.required}")
        @Size(min = 3, max = 100, message = "{validation.auth.fullName.size.regexp}")
        String fullName,

        @NotNull(message = "{validation.address.phone.required}")
        @PhoneNumber(message = "{validation.address.phone.pattern}")
        String phoneNumber
) {
}

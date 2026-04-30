package com.thang.roombooking.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class OtpVerifyRequest {
    @NotBlank(message = "{validation.auth.email.required}")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "{validation.auth.email.pattern}")
    @Size(max = 255, message = "{validation.auth.email.size}")
    @Schema(
            description = "Email address of the user",
            example = "abc@gmail.com"
    )
    private String email;

    @NotBlank(message = "{validation.auth.otp.required}")
    @Size(min = 6, max = 6, message = "{validation.auth.otp.size}")
    @Schema(
            description = "The OTP code sent to the user's email",
            example = "123456"
    )
    private String code;
}

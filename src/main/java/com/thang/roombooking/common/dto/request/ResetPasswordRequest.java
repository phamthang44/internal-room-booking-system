package com.thang.roombooking.common.dto.request;


import com.thang.roombooking.common.constant.CommonConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ResetPasswordRequest(
        @NotBlank(message = "{validation.auth.email.required}")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "{validation.auth.email.pattern}")
        @Size(max = CommonConfig.MAX_LENGTH_EMAIL, message = "{validation.auth.email.size}")
        @Schema(
                description = "Email address of the user",
                example = "abc@gmail.com"
        )
        String email,
        @NotBlank(message = "{validation.auth.otp.required}")
        @Size(min = 6, max = 6, message = "{validation.auth.otp.size}")
        @Schema(
                description = "The OTP code sent to the user's email",
                example = "123456"
        )
        String otp,
        @NotBlank(message = "{validation.auth.newPassword.required}")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{10,}$",
                message = "{validation.auth.password.pattern}"
        )
        @Schema(
                description = "The password of the user",
                example = "abc12345678!@#"
        )
        String newPassword
) {
}

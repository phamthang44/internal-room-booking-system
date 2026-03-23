package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.validator.PasswordMatches;
import com.thang.roombooking.common.validator.ValidIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@PasswordMatches
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "{validation.auth.username.required}")
    @Size(min = 3, max = 30, message = "{validation.auth.username.size}")
    private String username;

    @NotBlank(message = "{validation.auth.fullName.required}")
    @Size(min = 3, max = 100, message = "{validation.auth.fullName.size.regexp}")
    private String fullName;

    @NotBlank(message = "Email is required")
    @ValidIdentifier(message = "{validation.auth.identifier.invalid}")
    private String email;

    @NotBlank(message = "{validation.auth.password.required}")
    @Size(min = 8, message = "{validation.auth.password.min.size}")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,64}$",
            message = "{validation.auth.password.pattern}"
    )
    private String password;

    @NotBlank(message = "{validation.auth.confirm.password.required}")
    private String confirmPassword;
}

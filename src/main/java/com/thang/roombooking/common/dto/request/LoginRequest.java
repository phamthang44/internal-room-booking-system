package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.validator.ValidIdentifier;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @ValidIdentifier(message = "{validation.auth.identifier.required}")
    private String identifier;

    @NotBlank(message = "{validation.auth.password.required}")
    private String password;
}

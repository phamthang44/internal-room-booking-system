package com.thang.roombooking.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE}) // Bắt buộc là TYPE để nhận vào cả Object RegisterRequest
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
@Documented
public @interface PasswordMatches {
    String message() default "{validation.auth.password.mismatch}"; // Dùng key i18n cho đồng bộ
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
package com.thang.roombooking.common.validator;

import com.thang.roombooking.common.dto.request.RegisterRequest;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatches, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest dto, ConstraintValidatorContext context) {
        boolean isValid = dto.getPassword() != null && dto.getPassword().equals(dto.getConfirmPassword());

        if (!isValid) {
            context.disableDefaultConstraintViolation();

            // Trả về template gốc: {validation.auth.password.mismatch}
            String template = context.getDefaultConstraintMessageTemplate();

            context.buildConstraintViolationWithTemplate(template) // Gửi đi cái Key kèm ngoặc nhọn
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return isValid;
    }
}
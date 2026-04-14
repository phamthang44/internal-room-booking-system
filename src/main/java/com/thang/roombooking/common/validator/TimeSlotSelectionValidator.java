package com.thang.roombooking.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TimeSlotSelectionValidator implements ConstraintValidator<ValidTimeSlotSelection, List<Integer>> {

    @Override
    public boolean isValid(List<Integer> value, ConstraintValidatorContext context) {
        // 1. Để @NotEmpty xử lý nếu null/empty
        if (value == null || value.isEmpty()) return true;

        return value.size() <= 2;
    }
}
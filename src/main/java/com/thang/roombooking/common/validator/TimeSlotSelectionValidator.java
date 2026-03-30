package com.thang.roombooking.common.validator;

import com.thang.roombooking.repository.TimeSlotRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TimeSlotSelectionValidator implements ConstraintValidator<ValidTimeSlotSelection, List<Integer>> {

    private final TimeSlotRepository timeSlotRepository; // inject nếu cần check DB

    @Override
    public boolean isValid(List<Integer> value, ConstraintValidatorContext context) {
        // 1. Để @NotEmpty xử lý nếu null/empty
        if (value == null || value.isEmpty()) return true;

        return value.size() <= 2;
    }
}
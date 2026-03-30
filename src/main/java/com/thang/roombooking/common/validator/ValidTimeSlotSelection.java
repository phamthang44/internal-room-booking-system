package com.thang.roombooking.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeSlotSelectionValidator.class)
@Documented
public @interface ValidTimeSlotSelection {

    String message() default "{validation.booking.time_slot_ids.invalid_range}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
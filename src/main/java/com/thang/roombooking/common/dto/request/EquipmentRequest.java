package com.thang.roombooking.common.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EquipmentRequest(
        @NotNull(message = "{validation.equipment.id.required}")
        @Positive(message = "{validation.equipment.id.must_be_positive}")
        Integer id,

        @Min(value = 1, message = "{validation.equipment.quantity.min}")
        @Max(value = 10, message = "{validation.equipment.quantity.max}")
        @Positive(message = "{validation.equipment.quantity.must_be_positive}")
        Integer quantity
) {}

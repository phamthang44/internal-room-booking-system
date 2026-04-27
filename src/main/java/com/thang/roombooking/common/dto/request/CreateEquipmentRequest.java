package com.thang.roombooking.common.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateEquipmentRequest(
        @NotBlank(message = "{validation.equipment.name.vi.required}") String nameVi,
        @NotBlank(message = "{validation.equipment.name.en.required}") String nameEn,
        String descVi,
        String descEn
) {}

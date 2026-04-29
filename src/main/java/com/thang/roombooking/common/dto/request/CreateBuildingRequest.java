package com.thang.roombooking.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBuildingRequest(

        @NotBlank(message = "{validation.building.nameKey.required}")
        @Size(max = 100, message = "{validation.building.nameKey.size}")
        String nameKey,

        @NotBlank(message = "{validation.building.nameEn.required}")
        @Size(max = 255, message = "{validation.building.nameEn.size}")
        String nameEn,

        @NotBlank(message = "{validation.building.nameVi.required}")
        @Size(max = 255, message = "{validation.building.nameVi.size}")
        String nameVi,

        @NotBlank(message = "{validation.building.address.required}")
        @Size(max = 255, message = "{validation.building.address.size}")
        String address,

        boolean isActive
) {}

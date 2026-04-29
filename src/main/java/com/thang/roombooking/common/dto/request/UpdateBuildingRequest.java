package com.thang.roombooking.common.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateBuildingRequest(

        @Size(max = 255, message = "{validation.building.nameEn.size}")
        String nameEn,

        @Size(max = 255, message = "{validation.building.nameVi.size}")
        String nameVi,

        @Size(max = 255, message = "{validation.building.address.size}")
        String address
) {}

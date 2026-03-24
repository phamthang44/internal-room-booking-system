package com.thang.roombooking.common.dto.response;

import lombok.Builder;

@Builder
public record BasicBuildingResponse(
        Long id,
        String name,
        String address
) {
}

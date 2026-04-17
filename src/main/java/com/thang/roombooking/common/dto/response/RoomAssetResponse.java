package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.AssetType;
import lombok.Builder;

@Builder
public record RoomAssetResponse(
        Long id,
        String url,
        AssetType assetType,
        Boolean isPrimary
) {}


package com.thang.roombooking.service.policy.context;

import com.thang.roombooking.common.enums.BuildingAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class BuildingContext {
    private Long buildingId;
    private boolean currentIsActive;
    private BuildingAction action;
}

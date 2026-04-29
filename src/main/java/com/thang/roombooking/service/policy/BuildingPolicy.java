package com.thang.roombooking.service.policy;

import com.thang.roombooking.common.enums.BuildingAction;
import com.thang.roombooking.service.policy.context.BuildingContext;

public interface BuildingPolicy {
    BuildingAction getAction();
    void validate(BuildingContext context);
}

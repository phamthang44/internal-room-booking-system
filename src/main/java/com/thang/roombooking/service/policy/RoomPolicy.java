package com.thang.roombooking.service.policy;

import com.thang.roombooking.common.enums.RoomAction;
import com.thang.roombooking.service.policy.context.RoomContext;

public interface RoomPolicy {
    RoomAction getAction();
    void validate(RoomContext context);
}
package com.thang.roombooking.service.policy;

import com.thang.roombooking.common.enums.RoomAction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RoomPolicyFactory {

    private final Map<RoomAction, RoomPolicy> policyMap;

    public RoomPolicyFactory(List<RoomPolicy> policies) {
        this.policyMap = policies.stream()
                .collect(Collectors.toMap(RoomPolicy::getAction, Function.identity()));
    }

    public RoomPolicy getPolicy(RoomAction action) {
        RoomPolicy policy = policyMap.get(action);

        if (policy == null) {
            throw new IllegalArgumentException("No policy for action: " + action);
        }

        return policy;
    }
}

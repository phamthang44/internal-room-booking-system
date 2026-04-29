package com.thang.roombooking.service.policy;

import com.thang.roombooking.common.enums.BuildingAction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BuildingPolicyFactory {

    private final Map<BuildingAction, BuildingPolicy> policyMap;

    public BuildingPolicyFactory(List<BuildingPolicy> policies) {
        this.policyMap = policies.stream()
                .collect(Collectors.toMap(BuildingPolicy::getAction, Function.identity()));
    }

    public BuildingPolicy getPolicy(BuildingAction action) {
        BuildingPolicy policy = policyMap.get(action);
        if (policy == null) throw new IllegalArgumentException("No building policy for action: " + action);
        return policy;
    }
}

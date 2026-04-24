package com.thang.roombooking.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ViolationCreatedEvent {
    private final Long violationId;
    private final Long userId;
}

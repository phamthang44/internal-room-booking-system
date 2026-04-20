package com.thang.roombooking.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    BOOKING_STATUS("BOOKING_STATUS"),
    SYSTEM_ALERT("SYSTEM_ALERT");

    private final String value;
}

package com.thang.roombooking.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    BOOKING_STATUS("BOOKING_STATUS"),
    BOOKING_PENDING("BOOKING_PENDING"),
    BOOKING_APPROVED("BOOKING_APPROVED"),
    BOOKING_REJECTED("BOOKING_REJECTED"),
    BOOKING_CANCELLED("BOOKING_CANCELLED"),
    BOOKING_CHECKIN("BOOKING_CHECKIN"),
    BOOKING_CHECKOUT("BOOKING_CHECKOUT"),
    SYSTEM_ALERT("SYSTEM_ALERT");

    private final String value;
}

package com.thang.roombooking.common.enums;

/**
 * Slot-level status used for availability grids (room schedule).
 * This is NOT the same as room status (AVAILABLE/MAINTENANCE/...) nor booking status.
 */
public enum SlotBookingStatus {
    AVAILABLE,
    RESERVED,
    PENDING,
    APPROVED,
    OCCUPIED,
    IN_USE,
    REJECTED
}


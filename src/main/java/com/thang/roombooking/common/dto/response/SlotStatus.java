package com.thang.roombooking.common.dto.response;

import java.time.LocalTime;

public record SlotStatus(
        Long slotId,
        String slotName,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        boolean isAvailable, // Quan trọng nhất để FE render màu xanh/đỏ
        Long currentBookingId // (Optional) Để Admin biết ai đang chiếm chỗ
) {}
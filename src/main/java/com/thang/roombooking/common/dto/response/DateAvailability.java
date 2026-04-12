package com.thang.roombooking.common.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DateAvailability(
        LocalDate date,
        List<SlotStatus> slots // Danh sách toàn bộ các slot trong ngày đó
) {}
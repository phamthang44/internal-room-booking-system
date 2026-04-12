package com.thang.roombooking.common.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ClassroomAvailabilityResponse(
        LocalDate date,
        Boolean isFull,
        List<DateAvailability> availabilities
) {}
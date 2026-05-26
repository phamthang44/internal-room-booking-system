package com.thang.roombooking.fixture;

import com.thang.roombooking.entity.TimeSlot;

import java.time.LocalTime;
import java.util.List;

public final class TimeSlotFixtures {

    private TimeSlotFixtures() {}

    public static TimeSlot slot(int id, int startHour, int startMin, int endHour, int endMin) {
        return TimeSlot.builder()
                .id(id)
                .startTime(LocalTime.of(startHour, startMin))
                .endTime(LocalTime.of(endHour, endMin))
                .build();
    }

    public static TimeSlot slot1() {
        return slot(1, 7, 0, 9, 0);
    }

    public static TimeSlot slot2() {
        return slot(2, 9, 30, 11, 30);
    }

    public static TimeSlot slot3() {
        return slot(3, 13, 0, 15, 0);
    }

    public static TimeSlot slot4() {
        return slot(4, 15, 30, 17, 30);
    }

    public static List<TimeSlot> consecutiveSlots() {
        return List.of(slot1(), slot2());
    }

    public static List<TimeSlot> nonConsecutiveSlots() {
        return List.of(slot1(), slot3());
    }
}

package com.thang.roombooking.common.enums;

import java.util.Arrays;
import java.util.List;

public enum TranslatableEntityType {
    BUILDING,
    EQUIPMENT,
    ROOM_TYPE,
    CLASSROOM,
    TIME_SLOT;

    public static List<String> names(TranslatableEntityType... types) {
        return Arrays.stream(types)
                .map(Enum::name)
                .toList();
    }
}

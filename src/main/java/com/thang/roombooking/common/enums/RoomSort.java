package com.thang.roombooking.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum RoomSort {
    NEWEST("newest"),
    ROOM_NAME_ASC("room_name_asc"),
    ROOM_NAME_DESC("room_name_desc"),
    CAPACITY_ASC("capacity_asc"),
    CAPACITY_DESC("capacity_desc"),
    ;
    private final String value;

    RoomSort(String value) {
        this.value = value;
    }
    @JsonValue
    public String getValue() {
        return value;
    }
    @JsonCreator
    public static RoomSort fromValue(String value) {
        for (RoomSort sort : values()) {
            if (sort.value.equalsIgnoreCase(value)) {
                return sort;
            }
        }
        throw new IllegalArgumentException("Unknown sort: " + value);
    }
}

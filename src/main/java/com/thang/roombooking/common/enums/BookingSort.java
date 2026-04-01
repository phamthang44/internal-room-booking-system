package com.thang.roombooking.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BookingSort {
    NEWEST("newest"),
    BOOKING_DATE_ASC("booking_date_asc"),
    BOOKING_DATE_DESC("booking_date_desc"),
    STATUS_ASC("status_asc"),
    STATUS_DESC("status_desc"),
    ;

    private final String value;

    BookingSort(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BookingSort fromValue(String value) {
        for (BookingSort s : values()) {
            if (s.value.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown booking sort: " + value);
    }
}

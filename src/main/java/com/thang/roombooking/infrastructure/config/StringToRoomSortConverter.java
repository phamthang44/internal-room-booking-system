package com.thang.roombooking.infrastructure.config;

import com.thang.roombooking.common.enums.RoomSort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToRoomSortConverter implements Converter<String, RoomSort> {

    @Override
    public RoomSort convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return RoomSort.fromValue(source);
    }
}

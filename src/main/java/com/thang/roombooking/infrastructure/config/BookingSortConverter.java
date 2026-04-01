package com.thang.roombooking.infrastructure.config;

import com.thang.roombooking.common.enums.BookingSort;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class BookingSortConverter implements Converter<String, BookingSort> {

    @Override
    public BookingSort convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return BookingSort.fromValue(source);
    }
}

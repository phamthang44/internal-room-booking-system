package com.thang.roombooking.common.utils;

import com.thang.roombooking.common.enums.TranslatableEntityType;

public class TranslationKeyBuilder {

    private TranslationKeyBuilder() {}

    public static String build(TranslatableEntityType type, Long id, String field) {
        return type.name() + "_" + id + "_" + field;
    }
}
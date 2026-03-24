package com.thang.roombooking.service;

import com.thang.roombooking.common.enums.TranslatableEntityType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TranslationService {

    // Dịch cho 1 Entity đơn lẻ (Dùng cho Create/Update Response)
    String getTranslation(TranslatableEntityType type, Long entityId, String fieldName);

    // Dịch cho danh sách (Dùng cho Search/List)
    Map<String, String> getTranslations(Map<TranslatableEntityType, Set<Long>> idsByType);

}

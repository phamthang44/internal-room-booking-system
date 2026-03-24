package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.entity.Translation;
import com.thang.roombooking.repository.TranslationRepository;
import com.thang.roombooking.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {

    private final TranslationRepository translationRepository;

    @Override
    public String getTranslation(TranslatableEntityType type, Long entityId, String fieldName) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        return translationRepository.findByEntityTypeAndEntityIdAndLocaleAndFieldName(
                        type.name(), entityId, locale.isEmpty() ? "en" : locale, fieldName)
                .map(Translation::getContent)
                .orElse(null); // Trả về null để bên ngoài tự fallback về nameKey
    }

    @Override
    public Map<String, String> getTranslations(Map<TranslatableEntityType, Set<Long>> idsByType) {
        if (idsByType == null || idsByType.isEmpty()) return Collections.emptyMap();

        String locale = LocaleContextHolder.getLocale().getLanguage();
        String finalLocale = locale.isEmpty() ? "en" : locale;
        Map<String, String> results = new HashMap<>();

        // Duyệt qua từng loại Entity để query chính xác, tránh xung đột ID
        idsByType.forEach((type, ids) -> {
            if (ids != null && !ids.isEmpty()) {
                List<Translation> tl = translationRepository.findByEntityTypeInAndEntityIdInAndLocale(
                        List.of(type.name()),
                        new ArrayList<>(ids),
                        finalLocale);

                tl.forEach(t -> results.put(
                        t.getEntityType() + "_" + t.getEntityId() + "_" + t.getFieldName(),
                        t.getContent()
                ));
            }
        });

        return results;
    }
}
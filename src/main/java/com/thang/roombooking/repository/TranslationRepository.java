package com.thang.roombooking.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.thang.roombooking.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    List<Translation> findByEntityTypeInAndEntityIdInAndLocale(List<String> types, List<Long> ids, String locale);

    Optional<Translation> findByEntityTypeAndEntityIdAndLocaleAndFieldName(
            String entityType,
            Long entityId,
            String locale,
            String fieldName
    );

    List<Translation> findByEntityTypeAndLocale(String name, String finalLocale);
}

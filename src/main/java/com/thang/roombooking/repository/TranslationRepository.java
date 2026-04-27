package com.thang.roombooking.repository;

import com.thang.roombooking.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Translation findByEntityTypeAndIdAndLocale(String entityType, Long id, String locale);

    @Modifying
    @Query(value = """
        INSERT INTO translations (entity_type, entity_id, locale, field_name, content)
        VALUES (:entityType, :entityId, :locale, :fieldName, :content)
        ON CONFLICT (entity_type, entity_id, locale, field_name)
        DO UPDATE SET content = EXCLUDED.content
        """, nativeQuery = true)
    void upsertTranslation(
        @Param("entityType") String entityType,
        @Param("entityId")   Long entityId,
        @Param("locale")     String locale,
        @Param("fieldName")  String fieldName,
        @Param("content")    String content
    );
}

package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.AdminEquipmentDetailResponse;
import com.thang.roombooking.common.dto.response.AdminEquipmentListResponse;
import com.thang.roombooking.common.dto.response.AuditResponse;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.EquipmentErrorCode;
import com.thang.roombooking.entity.Equipment;
import com.thang.roombooking.entity.Translation;
import com.thang.roombooking.repository.EquipmentRepository;
import com.thang.roombooking.repository.TranslationRepository;
import com.thang.roombooking.service.EquipmentQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EquipmentQueryServiceImpl implements EquipmentQueryService {

    private final EquipmentRepository equipmentRepository;
    private final TranslationRepository translationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminEquipmentListResponse> listEquipment(String keyword, Pageable pageable) {
        List<Object[]> rows = equipmentRepository.findAllForAdmin(
                keyword, pageable.getPageSize(), pageable.getOffset());

        if (rows.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0L);
        }

        // Collect equipment IDs
        Set<Long> equipmentIds = rows.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toSet());

        // Fetch translations for both locales directly — admin needs both vi and en
        Map<String, String> viTranslations = fetchTranslationMap(equipmentIds, "vi");
        Map<String, String> enTranslations = fetchTranslationMap(equipmentIds, "en");

        List<AdminEquipmentListResponse> content = rows.stream().map(row -> {
            Integer id = ((Number) row[0]).intValue();
            String nameKey = (String) row[1];
            boolean isActive = (row[3] == null);
            long classroomCount = ((Number) row[4]).longValue();

            String translationPrefix = TranslatableEntityType.EQUIPMENT.name() + "_" + id + "_";
            String nameVi = viTranslations.get(translationPrefix + "name");
            String nameEn = enTranslations.get(translationPrefix + "name");

            return new AdminEquipmentListResponse(id, nameKey, nameVi, nameEn, isActive, classroomCount);
        }).toList();

        long total = equipmentRepository.countForAdmin(keyword);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminEquipmentDetailResponse getEquipment(Integer id) {
        Equipment equipment = equipmentRepository.findById(id.longValue())
                .orElseThrow(() -> new AppException(EquipmentErrorCode.EQUIPMENT_NOT_FOUND));

        Set<Long> ids = Set.of(id.longValue());
        Map<String, String> viTranslations = fetchTranslationMap(ids, "vi");
        Map<String, String> enTranslations = fetchTranslationMap(ids, "en");

        String prefix = TranslatableEntityType.EQUIPMENT.name() + "_" + id + "_";
        String nameVi = viTranslations.get(prefix + "name");
        String nameEn = enTranslations.get(prefix + "name");
        String descVi = viTranslations.get(prefix + "description");
        String descEn = enTranslations.get(prefix + "description");

        boolean isActive = (equipment.getDeletedAt() == null);
        long classroomCount = equipmentRepository.countClassroomsForEquipment(id);

        AuditResponse audit = AuditResponse.builder()
                .createdAt(equipment.getCreatedAt())
                .build();

        return new AdminEquipmentDetailResponse(
                equipment.getId(),
                equipment.getNameKey(),
                nameVi,
                nameEn,
                descVi,
                descEn,
                isActive,
                classroomCount,
                audit
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private Map<String, String> fetchTranslationMap(Set<Long> ids, String locale) {
        List<Translation> rows = translationRepository.findByEntityTypeInAndEntityIdInAndLocale(
                List.of(TranslatableEntityType.EQUIPMENT.name()),
                List.copyOf(ids),
                locale);

        Map<String, String> map = new HashMap<>();
        rows.forEach(t -> map.put(
                t.getEntityType() + "_" + t.getEntityId() + "_" + t.getFieldName(),
                t.getContent()));
        return map;
    }
}

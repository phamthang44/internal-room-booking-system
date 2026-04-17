package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.AdminBuildingResponse;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.repository.BuildingRepository;
import com.thang.roombooking.service.BuildingQueryService;
import com.thang.roombooking.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BuildingQueryServiceImpl implements BuildingQueryService {

    private final BuildingRepository buildingRepository;
    private final TranslationService translationService;

    @Override
    public List<AdminBuildingResponse> retrieveAllBuildings() {

        Page<Building> buildings = buildingRepository.findAll(PageRequest.of(0, 50));
        Map<TranslatableEntityType, Set<Long>> idsByType = handleEntityIdsByType(buildings);
        return List.of();
    }

    private Map<String, String> buildTranslations(Building building) {

        Map<TranslatableEntityType, Set<Long>> ids = new HashMap<>();

        populateEntityIds(building, ids);

        return translationService.getTranslations(ids);
    }

    private AdminBuildingResponse buildingToResponse(Building building) {

        Map<String, String> translations = buildTranslations(building);


        return AdminBuildingResponse.builder()
                .build();
    }

    private Map<TranslatableEntityType, Set<Long>> handleEntityIdsByType(Page<Building> buildings) {
        Map<TranslatableEntityType, Set<Long>> idsByType = new EnumMap<>(TranslatableEntityType.class);
        for (Building b : buildings.getContent()) {
            // Building ID
            populateEntityIds(b, idsByType);

        }
        return idsByType;
    }

    private static void populateEntityIds(Building building, Map<TranslatableEntityType, Set<Long>> ids) {
        ids.computeIfAbsent(TranslatableEntityType.BUILDING, k -> new HashSet<>())
                .add(building.getId());
    }
}

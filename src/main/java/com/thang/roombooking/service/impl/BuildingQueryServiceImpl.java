package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.response.AdminBuildingResponse;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BuildingErrorCode;
import com.thang.roombooking.common.mapper.BuildingMapper;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.repository.BuildingRepository;
import com.thang.roombooking.service.BuildingQueryService;
import com.thang.roombooking.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BuildingQueryServiceImpl implements BuildingQueryService {

    private final BuildingRepository buildingRepository;
    private final TranslationService translationService;
    private final BuildingMapper buildingMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AdminBuildingResponse> retrieveAllBuildings() {
        log.info("{} | Retrieve all buildings", LogConstant.ACTION_START);
        List<Building> buildings = buildingRepository.findAllByOrderByIdAsc();

        if (buildings.isEmpty()) return List.of();

        Set<Long> ids = new HashSet<>();
        for (Building b : buildings) ids.add(b.getId());

        Map<String, String> translations = translationService.getTranslations(
                Map.of(TranslatableEntityType.BUILDING, ids)
        );

        return buildings.stream()
                .map(b -> buildingMapper.toAdminBuildingResponse(b, translations))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBuildingResponse getBuildingById(Long id) {
        log.info("{} | Get building by ID: {}", LogConstant.ACTION_START, id);
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(BuildingErrorCode.BUILDING_NOT_FOUND));

        Map<String, String> translations = translationService.getTranslations(
                Map.of(TranslatableEntityType.BUILDING, Set.of(id))
        );

        return buildingMapper.toAdminBuildingResponse(building, translations);
    }
}

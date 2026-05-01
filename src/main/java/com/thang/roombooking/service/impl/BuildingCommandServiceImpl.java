package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.CreateBuildingRequest;
import com.thang.roombooking.common.dto.request.UpdateBuildingRequest;
import com.thang.roombooking.common.dto.response.AdminBuildingResponse;
import com.thang.roombooking.common.enums.BuildingAction;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BuildingErrorCode;
import com.thang.roombooking.common.mapper.BuildingMapper;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.repository.BuildingRepository;
import com.thang.roombooking.service.BuildingCommandService;
import com.thang.roombooking.service.TranslationService;
import com.thang.roombooking.service.policy.BuildingPolicy;
import com.thang.roombooking.service.policy.BuildingPolicyFactory;
import com.thang.roombooking.service.policy.context.BuildingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BuildingCommandServiceImpl implements BuildingCommandService {

    private final BuildingRepository buildingRepository;
    private final BuildingMapper buildingMapper;
    private final TranslationService translationService;
    private final BuildingPolicyFactory policyFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminBuildingResponse createBuilding(CreateBuildingRequest req) {
        log.info("{} | Create Building | nameKey: {}", LogConstant.ACTION_START, req.nameKey());
        if (buildingRepository.existsByNameKey(req.nameKey())) {
            throw new AppException(BuildingErrorCode.BUILDING_NAME_EXISTS);
        }

        Building building = Building.builder()
                .nameKey(req.nameKey())
                .address(req.address())
                .isActive(req.isActive())
                .build();

        Building saved = buildingRepository.save(building);

        translationService.saveTranslations(
                TranslatableEntityType.BUILDING,
                saved.getId(),
                List.of(
                        new TranslationService.TranslationInput("en", "name", req.nameEn()),
                        new TranslationService.TranslationInput("vi", "name", req.nameVi())
                )
        );

        log.info("{} | Created Building ID: {}", LogConstant.ACTION_SUCCESS, saved.getId());
        return buildingMapper.toAdminBuildingResponse(saved, fetchTranslations(saved.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminBuildingResponse updateBuilding(Long id, UpdateBuildingRequest req) {
        log.info("{} | Update Building | ID: {}", LogConstant.ACTION_START, id);
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(BuildingErrorCode.BUILDING_NOT_FOUND));

        if (req.address() != null) building.setAddress(req.address());

        Building saved = buildingRepository.save(building);

        List<TranslationService.TranslationInput> inputs = buildTranslationInputs(req.nameEn(), req.nameVi());
        if (!inputs.isEmpty()) {
            translationService.saveTranslations(TranslatableEntityType.BUILDING, saved.getId(), inputs);
        }

        log.info("{} | Updated Building ID: {}", LogConstant.ACTION_SUCCESS, saved.getId());
        return buildingMapper.toAdminBuildingResponse(saved, fetchTranslations(saved.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBuilding(Long id) {
        log.info("{} | Delete Building | ID: {}", LogConstant.ACTION_START, id);
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(BuildingErrorCode.BUILDING_NOT_FOUND));

        BuildingPolicy policy = policyFactory.getPolicy(BuildingAction.DELETE);
        policy.validate(BuildingContext.builder()
                .buildingId(id)
                .currentIsActive(Boolean.TRUE.equals(building.getIsActive()))
                .action(BuildingAction.DELETE)
                .build());

        buildingRepository.delete(building);
        log.info("{} | Deleted Building ID: {}", LogConstant.ACTION_SUCCESS, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminBuildingResponse toggleStatus(Long id, boolean isActive) {
        log.info("{} | Toggle Building Status | ID: {}, isActive: {}", LogConstant.ACTION_START, id, isActive);
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(BuildingErrorCode.BUILDING_NOT_FOUND));

        if (!isActive) {
            BuildingPolicy policy = policyFactory.getPolicy(BuildingAction.DEACTIVATE);
            policy.validate(BuildingContext.builder()
                    .buildingId(id)
                    .currentIsActive(Boolean.TRUE.equals(building.getIsActive()))
                    .action(BuildingAction.DEACTIVATE)
                    .build());
        }

        building.setIsActive(isActive);
        Building saved = buildingRepository.save(building);

        log.info("{} | Building ID: {} status set to: {}", LogConstant.ACTION_SUCCESS, id, isActive);
        return buildingMapper.toAdminBuildingResponse(saved, fetchTranslations(saved.getId()));
    }

    private Map<String, String> fetchTranslations(Long buildingId) {
        return translationService.getTranslations(
                Map.of(TranslatableEntityType.BUILDING, Set.of(buildingId))
        );
    }

    private List<TranslationService.TranslationInput> buildTranslationInputs(String nameEn, String nameVi) {
        List<TranslationService.TranslationInput> inputs = new java.util.ArrayList<>();
        if (nameEn != null && !nameEn.isBlank()) {
            inputs.add(new TranslationService.TranslationInput("en", "name", nameEn));
        }
        if (nameVi != null && !nameVi.isBlank()) {
            inputs.add(new TranslationService.TranslationInput("vi", "name", nameVi));
        }
        return inputs;
    }
}

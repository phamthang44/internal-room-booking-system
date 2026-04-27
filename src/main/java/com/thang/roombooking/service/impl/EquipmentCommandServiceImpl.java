package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.CreateEquipmentRequest;
import com.thang.roombooking.common.dto.request.UpdateEquipmentRequest;
import com.thang.roombooking.common.dto.response.AdminEquipmentDetailResponse;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.EquipmentErrorCode;
import com.thang.roombooking.entity.Equipment;
import com.thang.roombooking.repository.EquipmentRepository;
import com.thang.roombooking.service.EquipmentCommandService;
import com.thang.roombooking.service.EquipmentQueryService;
import com.thang.roombooking.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EquipmentCommandServiceImpl implements EquipmentCommandService {

    private final EquipmentRepository equipmentRepository;
    private final TranslationService translationService;
    private final EquipmentQueryService equipmentQueryService;

    @Override
    @Transactional
    public AdminEquipmentDetailResponse createEquipment(CreateEquipmentRequest req) {
        log.info("{} | Create Equipment | nameEn: {}", LogConstant.ACTION_START, req.nameEn());
        try {
            String base = toNameKey(req.nameEn());
            String nameKey = base;
            int counter = 2;
            while (equipmentRepository.existsByNameKey(nameKey)) {
                nameKey = base + "_" + counter++;
            }

            Equipment equipment = Equipment.builder()
                    .nameKey(nameKey)
                    .descriptionKey(nameKey + ".desc")
                    .build();

            Equipment saved = equipmentRepository.save(equipment);

            translationService.saveTranslations(
                    TranslatableEntityType.EQUIPMENT,
                    Long.valueOf(saved.getId()),
                    List.of(
                            new TranslationService.TranslationInput("vi", "name", req.nameVi()),
                            new TranslationService.TranslationInput("en", "name", req.nameEn()),
                            new TranslationService.TranslationInput("vi", "description", req.descVi()),
                            new TranslationService.TranslationInput("en", "description", req.descEn())
                    )
            );

            log.info("{} | Created Equipment ID: {}", LogConstant.ACTION_SUCCESS, saved.getId());
            return equipmentQueryService.getEquipment(saved.getId());
        } catch (AppException e) {
            log.warn("{} | Failed to create equipment | Error: {}", LogConstant.ACTION_FAILED, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Error creating equipment | System Error.", LogConstant.SYS_ERROR, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public AdminEquipmentDetailResponse updateEquipment(Integer id, UpdateEquipmentRequest req) {
        log.info("{} | Update Equipment | ID: {}", LogConstant.ACTION_START, id);
        try {
            equipmentRepository.findById(id.longValue())
                    .orElseThrow(() -> new AppException(EquipmentErrorCode.EQUIPMENT_NOT_FOUND));

            translationService.saveTranslations(
                    TranslatableEntityType.EQUIPMENT,
                    Long.valueOf(id),
                    List.of(
                            new TranslationService.TranslationInput("vi", "name", req.nameVi()),
                            new TranslationService.TranslationInput("en", "name", req.nameEn()),
                            new TranslationService.TranslationInput("vi", "description", req.descVi()),
                            new TranslationService.TranslationInput("en", "description", req.descEn())
                    )
            );

            log.info("{} | Updated Equipment ID: {}", LogConstant.ACTION_SUCCESS, id);
            return equipmentQueryService.getEquipment(id);
        } catch (AppException e) {
            log.warn("{} | Failed to update equipment | Error: {}", LogConstant.ACTION_FAILED, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Error updating equipment | System Error.", LogConstant.SYS_ERROR, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deactivateEquipment(Integer id) {
        log.info("{} | Deactivate Equipment | ID: {}", LogConstant.ACTION_START, id);
        try {
            Equipment equipment = equipmentRepository.findById(id.longValue())
                    .orElseThrow(() -> new AppException(EquipmentErrorCode.EQUIPMENT_NOT_FOUND));

            if (equipmentRepository.isAssignedToAnyClassroom(id)) {
                throw new AppException(EquipmentErrorCode.EQUIPMENT_IN_USE);
            }

            equipmentRepository.delete(equipment);
            log.info("{} | Deactivated Equipment ID: {}", LogConstant.ACTION_SUCCESS, id);
        } catch (AppException e) {
            log.warn("{} | Failed to deactivate equipment | Error: {}", LogConstant.ACTION_FAILED, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Error deactivating equipment | System Error.", LogConstant.SYS_ERROR, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void reactivateEquipment(Integer id) {
        log.info("{} | Reactivate Equipment | ID: {}", LogConstant.ACTION_START, id);
        try {
            Equipment equipment = equipmentRepository.findByIdIncludingDeleted(id)
                    .orElseThrow(() -> new AppException(EquipmentErrorCode.EQUIPMENT_NOT_FOUND));

            if (equipment.getDeletedAt() == null) {
                log.info("{} | Equipment ID: {} is already active — no-op", LogConstant.ACTION_SUCCESS, id);
                return;
            }

            equipmentRepository.reactivateById(id);
            log.info("{} | Reactivated Equipment ID: {}", LogConstant.ACTION_SUCCESS, id);
        } catch (AppException e) {
            log.warn("{} | Failed to reactivate equipment | Error: {}", LogConstant.ACTION_FAILED, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Error reactivating equipment | System Error.", LogConstant.SYS_ERROR, e);
            throw e;
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private String toNameKey(String nameEn) {
        return "equipment." + nameEn.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");
    }
}

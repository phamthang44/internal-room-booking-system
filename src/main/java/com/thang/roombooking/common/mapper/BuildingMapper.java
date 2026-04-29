package com.thang.roombooking.common.mapper;

import com.thang.roombooking.common.dto.response.AdminBuildingResponse;
import com.thang.roombooking.common.dto.response.AuditResponse;
import com.thang.roombooking.common.dto.response.BasicBuildingResponse;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.utils.TranslationKeyBuilder;
import com.thang.roombooking.entity.Building;
import org.mapstruct.*;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BuildingMapper {

    default AdminBuildingResponse toAdminBuildingResponse(Building building, Map<String, String> translations) {
        if (building == null) return null;

        String key = TranslationKeyBuilder.build(TranslatableEntityType.BUILDING, building.getId(), "name");
        String name = (translations != null && translations.containsKey(key))
                ? translations.get(key)
                : building.getNameKey();

        return AdminBuildingResponse.builder()
                .id(building.getId())
                .buildingName(name)
                .address(building.getAddress())
                .isActive(Boolean.TRUE.equals(building.getIsActive()))
                .auditResponse(AuditResponse.builder()
                        .createdAt(building.getCreatedAt())
                        .updatedAt(building.getUpdatedAt())
                        .createdBy(building.getCreatedBy())
                        .updatedBy(building.getUpdatedBy())
                        .build())
                .build();
    }



    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "nameKey")
    @Mapping(target = "address", source = "address")
    BasicBuildingResponse toBasicBuildingResponse(Building building, @Context Map<String, String> translations);

    @AfterMapping
    default void translateBasicBuilding(
            @MappingTarget BasicBuildingResponse.BasicBuildingResponseBuilder responseBuilder,
            Building building,
            @Context Map<String, String> translations
    ) {
        if (building != null) {
            String key = TranslationKeyBuilder.build(
                    TranslatableEntityType.BUILDING,
                    building.getId(),
                    "name"
            );

            if (translations != null && translations.containsKey(key)) {
                responseBuilder.name(translations.get(key));
            }
        }
    }
}

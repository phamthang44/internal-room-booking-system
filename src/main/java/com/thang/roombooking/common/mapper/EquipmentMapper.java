package com.thang.roombooking.common.mapper;

import com.thang.roombooking.common.dto.response.EquipmentResponse;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.utils.TranslationKeyBuilder;
import com.thang.roombooking.entity.ClassroomEquipment;
import org.mapstruct.*;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EquipmentMapper {

    @Mapping(target = "name", source = "equipment.nameKey")
    @Mapping(target = "description", source = "equipment.descriptionKey")
    @Mapping(target = "id", source = "equipment.id")
    EquipmentResponse toEquipmentResponse(ClassroomEquipment classroomEquipment, @Context Map<String, String> translations);

    @AfterMapping
    default void translateEquipmentResponse(
            @MappingTarget EquipmentResponse response,
            ClassroomEquipment classroomEquipment,
            @Context Map<String, String> translations
    ) {
        if (classroomEquipment.getEquipment() != null) {

            Long id = Long.valueOf(classroomEquipment.getEquipment().getId());

            String nameKey = TranslationKeyBuilder.build(
                    TranslatableEntityType.EQUIPMENT,
                    id,
                    "name"
            );

            if (translations != null && translations.containsKey(nameKey)) {
                response.setName(translations.get(nameKey));
            }

            String descKey = TranslationKeyBuilder.build(
                    TranslatableEntityType.EQUIPMENT,
                    id,
                    "description"
            );

            if (translations != null && translations.containsKey(descKey)) {
                response.setDescription(translations.get(descKey));
            }
        }
    }
}

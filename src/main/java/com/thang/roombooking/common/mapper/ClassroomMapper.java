package com.thang.roombooking.common.mapper;

import org.mapstruct.*;

import com.thang.roombooking.common.dto.response.BasicClassroomResponse;
import com.thang.roombooking.common.dto.response.EquipmentResponse;
import com.thang.roombooking.entity.Classroom;
import com.thang.roombooking.entity.ClassroomEquipment;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClassroomMapper {

    @Mapping(target = "classroomId", source = "id")
    @Mapping(target = "buildingName", source = "building.nameKey")
    @Mapping(target = "equipments", source = "classroomEquipments")
    @Mapping(target = "roomType", source = "roomType.nameKey")
    BasicClassroomResponse toBasicResponse(Classroom classroom, @Context Map<String, String> translations);

    @Mapping(target = "name", source = "equipment.nameKey")
    @Mapping(target = "description", source = "equipment.descriptionKey")
    EquipmentResponse toEquipmentResponse(ClassroomEquipment classroomEquipment, @Context Map<String, String> translations);

    @AfterMapping
    default void translateBasicResponse(@MappingTarget BasicClassroomResponse response, Classroom classroom, @Context Map<String, String> translations) {
        if (classroom.getBuilding() != null) {
            String key = "BUILDING_" + classroom.getBuilding().getId() + "_name";
            if (translations.containsKey(key)) {
                response.setBuildingName(translations.get(key));
            }
        }
        if (classroom.getRoomType() != null) {
            String key = "ROOM_TYPE_" + classroom.getRoomType().getId() + "_name";
            if (translations.containsKey(key)) {
                response.setRoomType(translations.get(key));
            }
        }
    }

    @AfterMapping
    default void translateEquipmentResponse(@MappingTarget EquipmentResponse response, ClassroomEquipment classroomEquipment, @Context Map<String, String> translations) {
        if (classroomEquipment.getEquipment() != null) {
            String nameKey = "EQUIPMENT_" + classroomEquipment.getEquipment().getId() + "_name";
            if (translations.containsKey(nameKey)) {
                response.setName(translations.get(nameKey));
            }

            String descKey = "EQUIPMENT_" + classroomEquipment.getEquipment().getId() + "_description";
            if (translations.containsKey(descKey)) {
                response.setDescription(translations.get(descKey));
            }
        }
    }

}

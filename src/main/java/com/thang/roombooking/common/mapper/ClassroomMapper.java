package com.thang.roombooking.common.mapper;

import com.thang.roombooking.common.dto.request.CreateClassroomRequest;
import com.thang.roombooking.common.dto.request.UpdateClassroomRequest;
import com.thang.roombooking.common.dto.response.*;
import com.thang.roombooking.common.enums.AssetType;
import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.entity.*;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {EquipmentMapper.class})
public interface ClassroomMapper {

    @Mapping(target = "classroomId", source = "id")
    @Mapping(target = "buildingName", source = "building.nameKey")
    @Mapping(target = "equipments", source = "classroomEquipments")
    @Mapping(target = "roomType", source = "roomType.nameKey")
    ClassroomListResponse toBasicResponse(Classroom classroom, @Context Map<String, String> translations);

    @AfterMapping
    default void translateBasicResponse(@MappingTarget ClassroomListResponse response, Classroom classroom, @Context Map<String, String> translations) {
        if (classroom.getBuilding() != null) {
            String key = "BUILDING_" + classroom.getBuilding().getId() + "_name";
            if (translations != null && translations.containsKey(key)) {
                response.setBuildingName(translations.get(key));
            }
        }
        if (classroom.getRoomType() != null) {
            String key = "ROOM_TYPE_" + classroom.getRoomType().getId() + "_name";
            if (translations != null && translations.containsKey(key)) {
                response.setRoomType(translations.get(key));
            }
        }

        // Primary image for list cards
        if (classroom.getRoomAssets() != null && !classroom.getRoomAssets().isEmpty()) {
            List<RoomAsset> images = classroom.getRoomAssets().stream()
                    .filter(a -> a.getAssetType() == null || a.getAssetType() == AssetType.IMAGE)
                    .sorted(Comparator
                            .comparing((RoomAsset a) -> Boolean.TRUE.equals(a.getIsPrimary()))
                            .reversed()
                            .thenComparing(a -> a.getId() == null ? Long.MAX_VALUE : a.getId()))
                    .toList();

            if (!images.isEmpty()) {
                response.setImageUrl(images.get(0).getUrl());
            }
        }
    }

    @Mapping(target = "roomAssets", ignore = true)
    @Mapping(target = "classroomEquipments", ignore = true)
    Classroom toEntityClassroom(CreateClassroomRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roomAssets", ignore = true)
    @Mapping(target = "classroomEquipments", ignore = true)
    void updateEntityFromRequest(UpdateClassroomRequest request, @MappingTarget Classroom classroom);

    CreateClassroomResponse toCreateClassroomResponse(Classroom classroom);

    UpdateClassroomResponse toUpdateClassroomResponse(Classroom classroom);

    @AfterMapping
    default void linkAssetsAndStatus(@MappingTarget Classroom classroom, CreateClassroomRequest request) {
        // 1. Map Status dựa trên isActive
        classroom.setStatus(request.isActive() ? RoomStatus.AVAILABLE : RoomStatus.INACTIVE);

        // 2. Map RoomAssets từ imageUrls (Dùng method addAsset đã có trong Entity để sync 2 chiều)
        if (request.imageUrls() != null) {
            for (int i = 0; i < request.imageUrls().size(); i++) {
                String url = request.imageUrls().get(i);
                RoomAsset asset = RoomAsset.builder()
                        .url(url)
                        .assetType(AssetType.IMAGE)
                        .isPrimary(i == 0)
                        .build();
                classroom.addAsset(asset); // Tận dụng method addAsset
            }
        }
    }
}

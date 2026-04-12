package com.thang.roombooking.common.mapper;

import com.thang.roombooking.common.dto.response.BasicRoomTypeResponse;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.utils.TranslationKeyBuilder;
import com.thang.roombooking.entity.RoomType;
import org.mapstruct.*;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomTypeMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "nameKey")
    BasicRoomTypeResponse toBasicRoomTypeResponse(RoomType roomType, @Context Map<String, String> translations);

    @AfterMapping
    default void translateRoomType(
            @MappingTarget BasicRoomTypeResponse response,
            RoomType roomType,
            @Context Map<String, String> translations
    ) {
        if (roomType != null) {
            String key = TranslationKeyBuilder.build(
                    TranslatableEntityType.ROOM_TYPE,
                    roomType.getId(),
                    "name"
            );

            if (translations != null && translations.containsKey(key)) {
                response.setName(translations.get(key));
            }
        }
    }
}

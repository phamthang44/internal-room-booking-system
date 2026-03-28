package com.thang.roombooking.common.mapper;

import com.thang.roombooking.common.dto.response.BasicRoomTypeResponse;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.common.dto.response.TimeSlotResponse;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.utils.TranslationKeyBuilder;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingTimeSlot;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.entity.TimeSlot;
import org.mapstruct.*;
import org.springframework.cache.annotation.Cacheable;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    @Mapping(target = "bookingId", source = "id")
    @Mapping(target = "building", source = "classroom.building")
    @Mapping(target = "bookingStatus", source = "status")
    @Mapping(target = "timeSlots", source = "bookingTimeSlots")
    @Mapping(target = "roomName", source = "classroom.roomName")
    CreateBookingResponse toCreateBookingResponse(Booking booking, @Context Map<String, String> translations);

    // 2. Mapping cho Building: Phải có method này MapStruct mới hiểu
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "nameKey")
    BasicRoomTypeResponse toBuildingResponse(Building building, @Context Map<String, String> buildingTranslations);

    // 3. Xử lý dịch Building sau khi map
    @AfterMapping
    default void translateBuilding(Building building,
                                   @MappingTarget BasicRoomTypeResponse response,
                                   @Context Map<String, String> buildingTranslations) {
        String key = "BUILDING_" + building.getId() + "_name"; // Khớp logic TranslationServiceImpl
        if (buildingTranslations != null && buildingTranslations.containsKey(key)) {
            response.setName(buildingTranslations.get(key));
        }
    }

    @Mapping(target = "id", source = "timeSlot.id")
    @Mapping(target = "startTime", source = "timeSlot.startTime")
    @Mapping(target = "endTime", source = "timeSlot.endTime")
    @Mapping(target = "slotName", source = "timeSlot.slotNameKey")
    TimeSlotResponse toTimeSlotResponse(BookingTimeSlot bookingTimeSlot, @Context Map<String, String> translations);

    @AfterMapping
    default void mapSlotName(BookingTimeSlot bts,
                             @MappingTarget TimeSlotResponse.TimeSlotResponseBuilder target,
                             @Context Map<String, String> translations) {

        TimeSlot slot = bts.getTimeSlot();
        // Tạo key khớp với logic trong TranslationService
        String lookupKey = "TIME_SLOT_" + slot.getId() + "_slotName";

        // Nếu không tìm thấy bản dịch trong DB, fallback về chính cái NameKey hoặc RoomName
        String translatedName = translations.getOrDefault(lookupKey, slot.getSlotNameKey());
        target.slotName(translatedName);
    }

}

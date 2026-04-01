package com.thang.roombooking.common.mapper;

import com.thang.roombooking.common.dto.response.BasicRoomTypeResponse;
import com.thang.roombooking.common.dto.response.BookingApprovalResponse;
import com.thang.roombooking.common.dto.response.BookingDetailResponse;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.common.dto.response.TimeSlotResponse;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingApproval;
import com.thang.roombooking.entity.BookingTimeSlot;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.entity.TimeSlot;
import org.mapstruct.*;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    // ── CreateBookingResponse ────────────────────────────────────────────────

    @Mapping(target = "bookingId", source = "id")
    @Mapping(target = "building", source = "classroom.building")
    @Mapping(target = "bookingStatus", source = "status")
    @Mapping(target = "timeSlots", source = "bookingTimeSlots")
    @Mapping(target = "roomName", source = "classroom.roomName")
    CreateBookingResponse toCreateBookingResponse(Booking booking, @Context Map<String, String> translations);

    // ── Building helper ──────────────────────────────────────────────────────

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "nameKey")
    BasicRoomTypeResponse toBuildingResponse(Building building, @Context Map<String, String> buildingTranslations);

    @AfterMapping
    default void translateBuilding(Building building,
                                   @MappingTarget BasicRoomTypeResponse response,
                                   @Context Map<String, String> buildingTranslations) {
        String key = "BUILDING_" + building.getId() + "_name";
        if (buildingTranslations != null && buildingTranslations.containsKey(key)) {
            response.setName(buildingTranslations.get(key));
        }
    }

    // ── TimeSlotResponse ─────────────────────────────────────────────────────

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
        String lookupKey = "TIME_SLOT_" + slot.getId() + "_slotName";
        String translatedName = translations.getOrDefault(lookupKey, slot.getSlotNameKey());
        target.slotName(translatedName);
    }

    // ── BookingDetailResponse ────────────────────────────────────────────────

    /**
     * Maps a {@link Booking} entity to a {@link BookingDetailResponse}.
     * <p>
     * The {@code translations} context map is used by {@link #afterMappingBookingDetail}
     * to resolve building name and time-slot name translations.
     *
     * @param booking      the booking entity (must have classroom + building eager/fetched)
     * @param translations combined translation map (building + time-slot entries)
     */
    @Mapping(target = "bookingId",       source = "id")
    @Mapping(target = "roomName",        source = "classroom.roomName")
    @Mapping(target = "buildingName",    source = "classroom.building.nameKey")
    @Mapping(target = "buildingAddress", source = "classroom.building.address")
    @Mapping(target = "bookingDate",     source = "bookingDate")
    @Mapping(target = "purpose",         source = "purpose")
    @Mapping(target = "attendees",       source = "attendees")   // no attendees column on Booking; set via @AfterMapping
    @Mapping(target = "status",          source = "status")
    @Mapping(target = "timeSlots",       ignore = true)   // translated & set explicitly in BookingQueryServiceImpl
    @Mapping(target = "approvalHistory", ignore = true)   // populated by service layer after separate query
    BookingDetailResponse toBookingDetailResponse(Booking booking, @Context Map<String, String> translations);

    @AfterMapping
    default void afterMappingBookingDetail(Booking booking,
                                           @MappingTarget BookingDetailResponse response,
                                           @Context Map<String, String> translations) {
        // Translate building name only – time-slot translation is handled by the service layer
        // because TimeSlotResponse is an immutable record and cannot be mutated post-build.
        if (booking.getClassroom() != null && booking.getClassroom().getBuilding() != null) {
            Building building = booking.getClassroom().getBuilding();
            String key = "BUILDING_" + building.getId() + "_name";
            if (translations != null && translations.containsKey(key)) {
                response.setBuildingName(translations.get(key));
            }
        }
    }

    // ── BookingApprovalResponse ──────────────────────────────────────────────

    @Mapping(target = "approvalId",     source = "id")
    @Mapping(target = "approverName",   source = "approver.email")
    @Mapping(target = "approvalStatus", source = "approvalStatus")
    @Mapping(target = "note",           source = "note")
    @Mapping(target = "decidedAt",      source = "createdAt")
    BookingApprovalResponse toBookingApprovalResponse(BookingApproval bookingApproval);
}

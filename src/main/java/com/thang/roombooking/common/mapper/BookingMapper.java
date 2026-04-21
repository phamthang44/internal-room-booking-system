package com.thang.roombooking.common.mapper;

import com.thang.roombooking.common.dto.response.*;
import com.thang.roombooking.entity.*;
import org.mapstruct.*;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {BuildingMapper.class})
public interface BookingMapper {

    // ── CreateBookingResponse ────────────────────────────────────────────────

    @Mapping(target = "bookingId", source = "id")
    @Mapping(target = "building", source = "classroom.building")
    @Mapping(target = "bookingStatus", source = "status")
    @Mapping(target = "timeSlots", source = "bookingTimeSlots")
    @Mapping(target = "roomName", source = "classroom.roomName")
    CreateBookingResponse toCreateBookingResponse(Booking booking, @Context Map<String, String> translations);

    // ── TimeSlotResponse ─────────────────────────────────────────────────────

    @Mapping(target = "id", source = "timeSlot.id")
    @Mapping(target = "startTime", expression = "java(bookingTimeSlot.getTimeSlot().getStartTime())")
    @Mapping(target = "endTime", expression = "java(bookingTimeSlot.getTimeSlot().getEndTime())")
    @Mapping(target = "slotName", source = "timeSlot.slotNameKey")
    TimeSlotResponse toTimeSlotResponse(BookingTimeSlot bookingTimeSlot, @Context Map<String, String> translations);

    @AfterMapping
    default void mapSlotName(BookingTimeSlot bts,
                             @MappingTarget TimeSlotResponse.TimeSlotResponseBuilder target,
                             @Context Map<String, String> translations) {

        TimeSlot slot = bts.getTimeSlot();
        String lookupKey = "TIME_SLOT_" + slot.getId() + "_name";
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
    @Mapping(target = "checkinTime",     source = "checkinTime")
    @Mapping(target = "checkoutTime",    source = "checkoutTime")
    @Mapping(target = "timeSlots",       ignore = true)   // translated & set explicitly in BookingQueryServiceImpl
    @Mapping(target = "bookingHistorySummaryResponses", ignore = true)   // populated by service layer after separate query
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

    @Mapping(target = "approvalId",     source = "id")
    @Mapping(target = "bookingId",      source = "booking.id")
    @Mapping(target = "studentName",    source = "booking.user.fullName")
    @Mapping(target = "roomName",       source = "booking.classroom.roomName")
    @Mapping(target = "bookingDate",    source = "booking.bookingDate")
    @Mapping(target = "approvalStatus", source = "approvalStatus")
    @Mapping(target = "approverName",   source = "approver.fullName")
    @Mapping(target = "note",           source = "note")
    @Mapping(target = "decidedAt",      source = "createdAt")
    ApprovalSummaryResponse toApprovalSummaryResponse(BookingApproval bookingApproval);

    // ── Admin booking (list + detail) ─────────────────────────────────────────

    @Mapping(target = "roomName", source = "classroom.roomName")
    @Mapping(target = "capacity", source = "classroom.capacity")
    @Mapping(target = "requestedAttendees", source = "attendees")
    @Mapping(target = "actualAttendees", expression = "java(booking.getActualAttendees() == null ? 0 : booking.getActualAttendees().intValue())")
    AdminBookingRoomRequestedResponse toAdminBookingRoomRequestedResponse(Booking booking);

    @Mapping(target = "user", expression = "java(com.thang.roombooking.common.dto.response.UserBasicResponse.fromEntityWithStudentCode(booking.getUser()))")
    @Mapping(target = "adminClassroom", source = "booking")
    @Mapping(target = "rejectedReason", source = "rejectionReason")
    @Mapping(target = "audit", source = "booking")
    @Mapping(target = "checkInTime", source = "checkinTime")
    @Mapping(target = "checkOutTime", source = "checkoutTime")
    @Mapping(target = "timeSlots", ignore = true)
    AdminBookingDetailResponse toAdminBookingDetailResponse(Booking booking, @Context Map<String, String> translations);

    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    AuditResponse toAuditResponse(Booking booking);
}

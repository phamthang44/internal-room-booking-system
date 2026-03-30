package com.thang.roombooking.common.exception.errorcode;

import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ClassroomErrorCode implements BaseErrorCode {

    // Nhóm lỗi khi quản lý Classroom (Admin)
    ROOM_NAME_EXISTED(HttpStatus.BAD_REQUEST, "CR_001", "error.classroom.name.existed"),
    CAPACITY_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "CR_002", "validation.classroom.capacity.out_of_range"),
    ROOM_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "CR_003", "validation.classroom.room_type.not_found"),
    BUILDING_NOT_ACTIVE(HttpStatus.FORBIDDEN, "CR_004", "error.classroom.building.not_active"),


    EQUIPMENT_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "CR_005", "error.classroom.equipment.not_supported"),
    CANNOT_DELETE_ROOM_WITH_ACTIVE_BOOKINGS(HttpStatus.BAD_REQUEST, "CR_006", "error.classroom.cannot_delete_room_with_active_bookings"), 
    CANNOT_DEACTIVATE_ROOM_WITH_UPCOMING_BOOKINGS(
            HttpStatus.BAD_REQUEST,
            "CR_007",
            "error.classroom.cannot_deactivate_room_with_current_and_upcoming_bookings"),
    CANNOT_CHANGE_STATUS_OF_DELETED_ROOM(
            HttpStatus.BAD_REQUEST,
            "CR_008",
            "error.classroom.cannot_change_status_of_deleted_room"
    ),

    // Nhóm lỗi trạng thái phục vụ Booking (User/Admin)
    CLASSROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CR_005", "validation.booking.classroom.not_found"),
    CLASSROOM_UNDER_MAINTENANCE(HttpStatus.BAD_REQUEST, "CR_006", "error.classroom.under_maintenance"),
    CLASSROOM_IS_DELETED_OR_INACTIVE(HttpStatus.BAD_REQUEST, "CR_007", "error.classroom.deleted_or_inactive"),

    // Lỗi logic nghiệp vụ khác
    CAPACITY_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "CR_008", "error.classroom.capacity_not_enough");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageKey;

    @Override
    public String getMessage() {
        return messageKey;
    }

    public String format(Object... args) {
        try {
            if (args == null || args.length == 0) {
                return I18nUtils.get(messageKey);
            }
            return I18nUtils.get(messageKey, args);
        } catch (Exception _) {
            return messageKey;
        }
    }
}

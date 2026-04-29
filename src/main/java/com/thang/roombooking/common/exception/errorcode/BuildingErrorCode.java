package com.thang.roombooking.common.exception.errorcode;

import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BuildingErrorCode implements BaseErrorCode {

    BUILDING_NOT_FOUND(HttpStatus.NOT_FOUND, "BL_001", "error.building.not_found"),
    BUILDING_NAME_EXISTS(HttpStatus.CONFLICT, "BL_002", "error.building.name.exists"),
    CANNOT_DELETE_BUILDING_WITH_ACTIVE_CLASSROOMS(HttpStatus.BAD_REQUEST, "BL_003", "error.building.cannot_delete_with_active_classrooms"),
    CANNOT_DEACTIVATE_BUILDING_WITH_UPCOMING_BOOKINGS(HttpStatus.BAD_REQUEST, "BL_004", "error.building.cannot_deactivate_with_upcoming_bookings");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageKey;

    @Override
    public String getMessage() {
        return messageKey;
    }

    @Override
    public String format(Object... args) {
        try {
            if (args == null || args.length == 0) return I18nUtils.get(messageKey);
            return I18nUtils.get(messageKey, args);
        } catch (Exception _) {
            return messageKey;
        }
    }
}

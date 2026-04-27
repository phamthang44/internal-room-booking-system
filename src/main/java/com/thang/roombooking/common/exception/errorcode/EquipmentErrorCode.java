package com.thang.roombooking.common.exception.errorcode;

import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EquipmentErrorCode implements BaseErrorCode {

    EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EQ_001", "error.equipment.not_found"),
    EQUIPMENT_NAME_EXISTED(HttpStatus.CONFLICT, "EQ_002", "error.equipment.name_existed"),
    EQUIPMENT_IN_USE(HttpStatus.CONFLICT, "EQ_003", "error.equipment.in_use");

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

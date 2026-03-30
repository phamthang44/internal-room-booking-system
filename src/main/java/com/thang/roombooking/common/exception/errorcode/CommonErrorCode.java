package com.thang.roombooking.common.exception.errorcode;

import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements BaseErrorCode {
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_001", "error.internal_server_error"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_002", "error.database_error"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "SYS_005", "error.invalid_request"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "SYS_006", "error.method_not_allowed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "SYS_404", "error.resource_not_found"),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", "error.resource_conflict"),
    OAUTH_ERROR(HttpStatus.BAD_REQUEST, "SYS_006", "error.oauth_error"), DATA_INTEGRITY_ERROR(HttpStatus.CONFLICT, "SYS_007", "error.data_integrity_error" ),;

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
        } catch (Exception e) {
            return messageKey;
        }
    }
}

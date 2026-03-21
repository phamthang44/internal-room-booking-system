package com.thang.roombooking.common.exception;

import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Getter
@Slf4j
public class AppException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public AppException(BaseErrorCode errorCode, Object... args) {
        super(resolveMessage(errorCode, args));
        this.errorCode = errorCode != null ? errorCode : CommonErrorCode.INTERNAL_ERROR;
    }

    private static String resolveMessage(BaseErrorCode errorCode, Object... args) {
        if (errorCode == null) {
            return I18nUtils.get("error.unknown_error");
        }
        try {
            if (args == null || args.length == 0) {
                return I18nUtils.get(errorCode.getMessage());
            }
            return I18nUtils.get(errorCode.getMessage(), args);
        } catch (Exception e) {
            return errorCode.getMessage();
        }
    }

    public String getErrorCodeStr() {
        return errorCode.getCode();
    }

    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}
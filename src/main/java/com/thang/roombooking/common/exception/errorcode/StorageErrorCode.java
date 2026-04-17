package com.thang.roombooking.common.exception.errorcode;

import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StorageErrorCode implements BaseErrorCode {

    FILE_INVALID(HttpStatus.BAD_REQUEST, "STORAGE_001", "error.storage.file_invalid"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "STORAGE_002", "error.storage.file_type_not_allowed"),
    FILE_URL_INVALID(HttpStatus.BAD_REQUEST, "STORAGE_003", "error.storage.file_url_invalid"),

    UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_010", "error.storage.upload_failed"),
    UPLOAD_NO_URL(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_011", "error.storage.upload_no_url"),
    DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_020", "error.storage.delete_failed"),
    BULK_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_021", "error.storage.bulk_delete_failed"),
    REMOVE_TAG_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_030", "error.storage.remove_tag_failed"),
    ;

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
        } catch (Exception e) {
            return messageKey;
        }
    }
}


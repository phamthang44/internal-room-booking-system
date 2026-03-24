package com.thang.roombooking.common.exception.errorcode;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();
    String format(Object... args);
}

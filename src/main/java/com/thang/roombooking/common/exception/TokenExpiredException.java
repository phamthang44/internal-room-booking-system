package com.thang.roombooking.common.exception;

import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;

public class TokenExpiredException extends AppException {
    public TokenExpiredException(Object... messageArgs) {
        super(AuthErrorCode.TOKEN_EXPIRED, messageArgs);
    }
}

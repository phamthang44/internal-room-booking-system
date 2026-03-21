package com.thang.roombooking.common.exception;

public class TokenExpiredException extends AppException {
    public TokenExpiredException(Object... messageArgs) {
        super(AuthErrorCode.TOKEN_EXPIRED, messageArgs);
    }
}

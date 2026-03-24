package com.thang.roombooking.common.exception;

import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;

public class TokenRefreshException extends AppException {
    public TokenRefreshException(String token, String message) {
        super(AuthErrorCode.TOKEN_INVALID, token, message);
    }
}

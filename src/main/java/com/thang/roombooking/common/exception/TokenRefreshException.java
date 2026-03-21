package com.thang.roombooking.common.exception;

public class TokenRefreshException extends AppException {
    public TokenRefreshException(String token, String message) {
        super(AuthErrorCode.TOKEN_INVALID, token, message);
    }
}

package com.thang.roombooking.common.exception;



public class TokenErrorException extends AppException {
    public TokenErrorException(String detail) {
        super(CommonErrorCode.INTERNAL_ERROR, detail);
    }
}

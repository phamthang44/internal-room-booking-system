package com.thang.roombooking.common.exception;


import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;

public class TokenErrorException extends AppException {
    public TokenErrorException(String detail) {
        super(CommonErrorCode.INTERNAL_ERROR, detail);
    }
}
